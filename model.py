"""
model.py
Random Forest Classifier for predicting user fitness level (Low, Moderate, High).
Handles training, evaluation, persistence, and inference.
"""

import os
import joblib
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix

from data_processor import (
    load_and_preprocess_data,
    calculate_bmi,
    calculate_fitness_score,
    generate_recommendations
)

MODEL_FILE = "fitness_model.pkl"

ML_FEATURES = [
    'age',
    'gender_encoded',
    'height_cm',
    'weight_kg',
    'bmi',
    'daily_steps',
    'heart_rate',
    'sleep_hours',
    'exercise_mins',
    'water_liters',
    'calories_burned'
]


def train_model(csv_path="fitness_data.csv", save_path=MODEL_FILE):
    """
    Trains a Random Forest Classifier on the preprocessed fitness dataset.
    Saves model bundle including classes and feature column list.
    """
    print(f"[*] Loading and preprocessing data from {csv_path}...")
    df = load_and_preprocess_data(csv_path)

    X = df[ML_FEATURES]
    y = df['fitness_level']

    # Train / Test Split (80% Train, 20% Test)
    # Use stratify if classes have sufficient counts
    stratify = y if y.value_counts().min() >= 2 else None
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=stratify
    )

    print(f"[*] Training Random Forest Classifier on {len(X_train)} samples...")
    clf = RandomForestClassifier(
        n_estimators=100,
        max_depth=6,
        random_state=42,
        class_weight='balanced'
    )
    clf.fit(X_train, y_train)

    # Evaluate
    y_pred = clf.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    report = classification_report(y_test, y_pred, output_dict=True, zero_division=0)
    cm = confusion_matrix(y_test, y_pred)

    print(f"[+] Model Training Complete! Accuracy: {accuracy * 100:.2f}%")

    # Feature Importance
    importances = dict(zip(ML_FEATURES, clf.feature_importances_))

    model_bundle = {
        'model': clf,
        'features': ML_FEATURES,
        'classes': clf.classes_.tolist(),
        'accuracy': round(accuracy * 100, 2),
        'confusion_matrix': cm.tolist(),
        'feature_importances': {k: round(float(v), 4) for k, v in sorted(importances.items(), key=lambda item: item[1], reverse=True)}
    }

    joblib.dump(model_bundle, save_path)
    print(f"[+] Saved trained model bundle to '{save_path}'")
    return model_bundle


def load_model(model_path=MODEL_FILE):
    """
    Loads saved model bundle, or trains a new one if not yet created.
    """
    if not os.path.exists(model_path):
        print(f"[!] Model file '{model_path}' not found. Training now...")
        return train_model(save_path=model_path)
    return joblib.load(model_path)


def predict_fitness(input_data, model_path=MODEL_FILE):
    """
    Predicts fitness level for a single user record and computes scores.
    input_data: dictionary containing keys:
      age, gender, height_cm, weight_kg, daily_steps, heart_rate,
      sleep_hours, exercise_mins, water_liters, calories_burned
    """
    bundle = load_model(model_path)
    model = bundle['model']

    # Extract inputs with fallbacks
    age = float(input_data.get('age', 25))
    gender_str = str(input_data.get('gender', 'Male')).strip().capitalize()
    gender_encoded = 1 if gender_str in ['Male', 'M', '1'] else 0
    height_cm = float(input_data.get('height_cm', 170))
    weight_kg = float(input_data.get('weight_kg', 70))
    daily_steps = float(input_data.get('daily_steps', 7500))
    heart_rate = float(input_data.get('heart_rate', 70))
    sleep_hours = float(input_data.get('sleep_hours', 7.0))
    exercise_mins = float(input_data.get('exercise_mins', 30))
    water_liters = float(input_data.get('water_liters', 2.5))
    calories_burned = float(input_data.get('calories_burned', 2200))

    # Derived Metrics
    bmi = calculate_bmi(weight_kg, height_cm)
    fitness_score = calculate_fitness_score(
        daily_steps, exercise_mins, sleep_hours, water_liters, heart_rate, bmi
    )

    # Feature vector matching training columns
    feature_row = pd.DataFrame([{
        'age': age,
        'gender_encoded': gender_encoded,
        'height_cm': height_cm,
        'weight_kg': weight_kg,
        'bmi': bmi,
        'daily_steps': daily_steps,
        'heart_rate': heart_rate,
        'sleep_hours': sleep_hours,
        'exercise_mins': exercise_mins,
        'water_liters': water_liters,
        'calories_burned': calories_burned
    }])[ML_FEATURES]

    # Predict class & probabilities
    prediction = model.predict(feature_row)[0]
    probabilities = {}
    if hasattr(model, 'predict_proba'):
        prob_values = model.predict_proba(feature_row)[0]
        for cls_name, prob in zip(model.classes_, prob_values):
            probabilities[cls_name] = round(float(prob) * 100, 1)

    recommendations = generate_recommendations(
        daily_steps, exercise_mins, sleep_hours, water_liters, heart_rate, bmi, prediction
    )

    return {
        'predicted_level': prediction,
        'probabilities': probabilities,
        'bmi': bmi,
        'fitness_score': fitness_score,
        'recommendations': recommendations,
        'inputs': {
            'age': int(age),
            'gender': gender_str,
            'height_cm': height_cm,
            'weight_kg': weight_kg,
            'daily_steps': int(daily_steps),
            'heart_rate': int(heart_rate),
            'sleep_hours': sleep_hours,
            'exercise_mins': int(exercise_mins),
            'water_liters': water_liters,
            'calories_burned': int(calories_burned)
        }
    }


if __name__ == '__main__':
    train_model()
