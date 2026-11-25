"""
main.py
Flask web application controller for Apache HBase-Based Predictive Analytics System
for Fitness and Wellness.
"""

import os
import json
from flask import Flask, render_template, request, redirect, url_for, flash, jsonify
import pandas as pd

from data_processor import (
    load_and_preprocess_data,
    calculate_bmi,
    calculate_fitness_score
)
from model import train_model, load_model, predict_fitness, MODEL_FILE
from hbase_manager import HBaseManager

app = Flask(__name__)
app.secret_key = os.environ.get("FLASK_SECRET_KEY", "fitness-analytics-hbase-key-2026")

# Initialize HBase Manager (singleton)
hbase_manager = HBaseManager()

# Ensure model exists on startup
if not os.path.exists(MODEL_FILE):
    try:
        train_model()
    except Exception as e:
        print(f"[!] Warning: Initial model training deferred: {e}")


@app.context_processor
def inject_global_data():
    """Injects HBase connection state into all templates."""
    status = hbase_manager.get_status()
    return dict(hbase_status=status)


@app.route('/')
def index():
    """
    Main Dashboard: Displays aggregate analytics, model metrics,
    distribution stats, and HBase status.
    """
    try:
        df = load_and_preprocess_data()
        total_records = len(df)
        avg_bmi = round(df['bmi'].mean(), 1) if 'bmi' in df.columns else 23.5
        avg_fitness_score = round(df['fitness_score'].mean(), 1) if 'fitness_score' in df.columns else 65.0
        
        # Fitness level distribution from dataset
        level_counts = df['fitness_level'].value_counts().to_dict()
    except Exception as e:
        total_records = 0
        avg_bmi = 0
        avg_fitness_score = 0
        level_counts = {'High': 0, 'Moderate': 0, 'Low': 0}

    # Fetch recent predictions from HBase
    predictions = hbase_manager.get_all_predictions(limit=5)
    total_predictions = len(hbase_manager.get_all_predictions(limit=500))

    # Model accuracy
    try:
        bundle = load_model()
        model_accuracy = bundle.get('accuracy', 90.0)
        feature_importances = bundle.get('feature_importances', {})
    except Exception:
        model_accuracy = "N/A"
        feature_importances = {}

    return render_template(
        'index.html',
        total_records=total_records,
        total_predictions=total_predictions,
        avg_bmi=avg_bmi,
        avg_fitness_score=avg_fitness_score,
        model_accuracy=model_accuracy,
        level_counts=level_counts,
        feature_importances=feature_importances,
        recent_predictions=predictions
    )


@app.route('/predict', methods=['GET', 'POST'])
def predict():
    """
    Fitness Prediction: Handles input collection, validation,
    Random Forest inference, rule scoring, and HBase persistence.
    """
    if request.method == 'POST':
        try:
            # Form field extraction and validation
            form_data = {
                'age': float(request.form.get('age', 25)),
                'gender': request.form.get('gender', 'Male'),
                'height_cm': float(request.form.get('height_cm', 170)),
                'weight_kg': float(request.form.get('weight_kg', 70)),
                'daily_steps': float(request.form.get('daily_steps', 7500)),
                'heart_rate': float(request.form.get('heart_rate', 70)),
                'sleep_hours': float(request.form.get('sleep_hours', 7.0)),
                'exercise_mins': float(request.form.get('exercise_mins', 30)),
                'water_liters': float(request.form.get('water_liters', 2.5)),
                'calories_burned': float(request.form.get('calories_burned', 2200)),
                'stress_level': request.form.get('stress_level', 'Medium')
            }

            # Range validations
            if not (10 <= form_data['age'] <= 100):
                flash("Age must be between 10 and 100.", "danger")
                return render_template('predict.html', form_data=request.form)

            if not (100 <= form_data['height_cm'] <= 250):
                flash("Height must be between 100 cm and 250 cm.", "danger")
                return render_template('predict.html', form_data=request.form)

            if not (25 <= form_data['weight_kg'] <= 250):
                flash("Weight must be between 25 kg and 250 kg.", "danger")
                return render_template('predict.html', form_data=request.form)

            # Perform prediction
            result = predict_fitness(form_data)

            # Persist to Apache HBase
            hbase_result = hbase_manager.save_prediction(result)

            return render_template(
                'result.html',
                result=result,
                hbase_result=hbase_result
            )

        except ValueError as ve:
            flash(f"Invalid input: Please enter valid numerical values. ({ve})", "danger")
            return render_template('predict.html', form_data=request.form)
        except Exception as e:
            flash(f"Prediction failed: {str(e)}", "danger")
            return render_template('predict.html', form_data=request.form)

    return render_template('predict.html', form_data={})


@app.route('/history')
def history():
    """
    Displays historical predictions stored in Apache HBase (or fallback if HBase offline).
    """
    predictions = hbase_manager.get_all_predictions(limit=100)
    return render_template('history.html', predictions=predictions)


@app.route('/data')
def data():
    """
    Displays loaded dataset records with calculated BMI, Fitness Score, and Features.
    """
    try:
        df = load_and_preprocess_data()
        records = df.to_dict(orient='records')
        columns = df.columns.tolist()
    except Exception as e:
        records = []
        columns = []
        flash(f"Could not load dataset: {e}", "warning")

    return render_template('data.html', records=records, columns=columns)


@app.route('/retrain', methods=['POST'])
def retrain():
    """
    Triggers model retraining from updated CSV dataset.
    """
    try:
        bundle = train_model()
        flash(f"Model retrained successfully! Test accuracy: {bundle['accuracy']}%", "success")
    except Exception as e:
        flash(f"Retraining failed: {e}", "danger")
    return redirect(url_for('index'))


@app.route('/api/stats')
def api_stats():
    """
    Returns JSON stats for Chart.js visualizations.
    """
    try:
        df = load_and_preprocess_data()
        level_counts = df['fitness_level'].value_counts().to_dict()
        
        # Step intervals distribution
        step_bins = {
            '< 5,000': int((df['daily_steps'] < 5000).sum()),
            '5,000 - 8,000': int(((df['daily_steps'] >= 5000) & (df['daily_steps'] < 8000)).sum()),
            '8,000 - 10,000': int(((df['daily_steps'] >= 8000) & (df['daily_steps'] < 10000)).sum()),
            '> 10,000': int((df['daily_steps'] >= 10000).sum())
        }

        # Sleep vs Fitness Score sample points
        scatter_data = []
        for _, row in df.sample(min(30, len(df)), random_state=42).iterrows():
            scatter_data.append({
                'x': float(row['sleep_hours']),
                'y': float(row['fitness_score']),
                'level': str(row['fitness_level'])
            })

        return jsonify({
            'status': 'success',
            'levels': level_counts,
            'steps': step_bins,
            'scatter': scatter_data
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.errorhandler(404)
def not_found(e):
    return render_template('base.html', content="<div class='container py-5 text-center'><h2>404 - Page Not Found</h2><a href='/' class='btn btn-primary mt-3'>Return to Dashboard</a></div>"), 404


if __name__ == '__main__':
    # Run development server on port 5000
    app.run(host='0.0.0.0', port=5000, debug=True)
