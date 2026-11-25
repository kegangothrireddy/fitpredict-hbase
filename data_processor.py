"""
data_processor.py
Handles loading, inspecting, cleaning, mapping columns, BMI computation,
fitness score calculation, and feature engineering for the fitness dataset.
"""

import os
import pandas as pd
import numpy as np

# Canonical feature column names expected by the model
FEATURE_COLUMNS = [
    'age',
    'gender',          # 1 = Male, 0 = Female
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

TARGET_COLUMN = 'fitness_level'

# Common column name aliases for flexible CSV ingestion
COLUMN_ALIASES = {
    'age': ['age', 'user_age'],
    'gender': ['gender', 'sex'],
    'height_cm': ['height_cm', 'height', 'height(cm)', 'height_in_cm'],
    'weight_kg': ['weight_kg', 'weight', 'weight(kg)', 'weight_in_kg'],
    'daily_steps': ['daily_steps', 'steps', 'step_count', 'total_steps'],
    'heart_rate': ['heart_rate', 'resting_heart_rate', 'bpm', 'hr', 'resting_hr'],
    'sleep_hours': ['sleep_hours', 'sleep', 'sleep_duration', 'hours_of_sleep'],
    'exercise_mins': ['exercise_mins', 'exercise_time', 'workout_minutes', 'active_minutes', 'exercise_minutes'],
    'water_liters': ['water_liters', 'water_intake', 'water(l)', 'water_intake_liters', 'hydration'],
    'calories_burned': ['calories_burned', 'calories', 'active_calories', 'calorie_burn'],
    'fitness_level': ['fitness_level', 'target', 'category', 'fitness_category', 'class']
}


def calculate_bmi(weight_kg, height_cm):
    """
    Calculate Body Mass Index (BMI).
    Formula: weight (kg) / [height (m)]^2
    """
    try:
        weight = float(weight_kg)
        height = float(height_cm)
        if height <= 0 or weight <= 0:
            return 22.0
        height_m = height / 100.0
        bmi = weight / (height_m ** 2)
        return round(bmi, 2)
    except (ValueError, ZeroDivisionError, TypeError):
        return 22.0


def calculate_fitness_score(steps, exercise_mins, sleep_hours, water_liters, heart_rate, bmi, stress_level="Medium"):
    """
    Computes a transparent, documented fitness score between 0 and 100.
    DISCLAIMER: Non-medically validated heuristic score for demonstration and education.
    
    Weights:
    - Daily Steps (25 pts): 10,000 steps = 25 pts
    - Exercise Mins (20 pts): 45 mins = 20 pts
    - Sleep Hours (15 pts): 7 - 9 hours = 15 pts
    - Water Intake (15 pts): 3.0 liters = 15 pts
    - Resting Heart Rate (15 pts): 60-70 bpm = 15 pts
    - BMI (10 pts): 18.5 - 24.9 (Normal range) = 10 pts
    - Stress Level: Low (+5 pts), High (-5 pts)
    """
    score = 0.0

    # 1. Steps (max 25)
    step_ratio = min(max(float(steps), 0) / 10000.0, 1.0)
    score += step_ratio * 25.0

    # 2. Exercise (max 20)
    exercise_ratio = min(max(float(exercise_mins), 0) / 45.0, 1.0)
    score += exercise_ratio * 20.0

    # 3. Sleep (max 15)
    sleep = float(sleep_hours)
    if 7.0 <= sleep <= 9.0:
        score += 15.0
    elif 6.0 <= sleep < 7.0 or 9.0 < sleep <= 10.0:
        score += 10.0
    elif 5.0 <= sleep < 6.0:
        score += 6.0
    else:
        score += 2.0

    # 4. Water (max 15)
    water_ratio = min(max(float(water_liters), 0) / 3.0, 1.0)
    score += water_ratio * 15.0

    # 5. Heart Rate (max 15)
    hr = float(heart_rate)
    if 55 <= hr <= 72:
        score += 15.0
    elif 73 <= hr <= 80:
        score += 10.0
    elif 81 <= hr <= 90:
        score += 6.0
    else:
        score += 2.0

    # 6. BMI (max 10)
    bmi_val = float(bmi)
    if 18.5 <= bmi_val <= 24.9:
        score += 10.0
    elif 25.0 <= bmi_val <= 29.9:
        score += 6.0
    elif 17.0 <= bmi_val < 18.5:
        score += 5.0
    else:
        score += 2.0

    # 7. Stress Modifier
    stress_str = str(stress_level).strip().lower()
    if stress_str == "low":
        score += 5.0
    elif stress_str == "high":
        score -= 5.0

    return round(min(max(score, 0.0), 100.0), 1)


def score_to_category(score):
    """
    Rule-based fitness category based on transparent score.
    Low: < 50
    Moderate: 50 - 74.9
    High: >= 75
    """
    if score >= 75.0:
        return 'High'
    elif score >= 50.0:
        return 'Moderate'
    else:
        return 'Low'


def generate_recommendations(steps, exercise_mins, sleep_hours, water_liters, heart_rate, bmi, fitness_level):
    """
    Generates personalized, non-medical lifestyle and fitness tips.
    """
    recommendations = []

    # Steps & Activity
    if steps < 6000:
        recommendations.append("Increase daily movement: Aim for a short 15-minute brisk walk after meals to reach at least 7,500 daily steps.")
    elif steps < 10000:
        recommendations.append("Good step count! Add light evening walks or use stairs to reach the optimal 10,000 steps milestone.")
    else:
        recommendations.append("Outstanding step volume! Maintain this daily consistency.")

    # Exercise
    if exercise_mins < 20:
        recommendations.append("Incorporate at least 30 minutes of moderate aerobic or bodyweight exercise 4-5 times a week.")
    elif exercise_mins < 45:
        recommendations.append("Solid workout commitment. Consider adding resistance or strength training 2-3 days a week.")
    else:
        recommendations.append("Great training frequency! Remember to schedule dedicated rest and mobility days.")

    # Sleep
    if sleep_hours < 6.5:
        recommendations.append("Your sleep duration is lower than recommended. Prioritize 7 to 8 hours of restorative sleep for muscle recovery.")
    elif sleep_hours > 9.5:
        recommendations.append("Excessive sleep duration detected. Try keeping a consistent wake-up schedule each morning.")
    else:
        recommendations.append("Optimal sleep hours! Quality sleep significantly boosts cellular repair and cognitive energy.")

    # Hydration
    if water_liters < 2.2:
        recommendations.append("Boost hydration: Carry a 1L water bottle and drink regularly throughout the day (target 2.5 - 3.5 Liters).")
    else:
        recommendations.append("Well hydrated! Proper fluid intake aids metabolic function and athletic performance.")

    # Heart Rate & BMI
    if heart_rate > 80:
        recommendations.append("Resting heart rate is slightly elevated. Gentle cardio, yoga, and breathing exercises can help lower resting BPM.")
    
    if bmi > 25.0:
        recommendations.append("BMI is in the overweight zone. Focus on whole-food nutrition, portion awareness, and calorie-deficit cardio.")
    elif bmi < 18.5:
        recommendations.append("BMI is in the underweight range. Consider nutrient-dense meals with adequate protein to build lean mass.")

    return recommendations


def inspect_and_map_columns(df):
    """
    Maps uploaded DataFrame columns to required canonical names.
    """
    col_mapping = {}
    lower_cols = {c.strip().lower(): c for c in df.columns}

    for canonical_name, aliases in COLUMN_ALIASES.items():
        found = False
        for alias in aliases:
            if alias.lower() in lower_cols:
                col_mapping[lower_cols[alias.lower()]] = canonical_name
                found = True
                break

    mapped_df = df.rename(columns=col_mapping)
    return mapped_df


def load_and_preprocess_data(csv_path="fitness_data.csv"):
    """
    Loads, cleans, and standardizes the fitness dataset.
    Returns: cleaned DataFrame and metadata dict.
    """
    if not os.path.exists(csv_path):
        raise FileNotFoundError(f"Dataset '{csv_path}' not found.")

    raw_df = pd.read_csv(csv_path)
    df = inspect_and_map_columns(raw_df)

    # Validate gender column and encode (Male -> 1, Female -> 0)
    if 'gender' in df.columns:
        df['gender'] = df['gender'].astype(str).str.strip().str.capitalize()
        # Create numeric gender representation for ML model
        df['gender_encoded'] = df['gender'].apply(lambda x: 1 if x in ['Male', 'M', '1'] else 0)
    else:
        df['gender'] = 'Unknown'
        df['gender_encoded'] = 1

    # Ensure numeric columns are converted safely
    numeric_cols = ['age', 'height_cm', 'weight_kg', 'daily_steps', 'heart_rate', 
                    'sleep_hours', 'exercise_mins', 'water_liters', 'calories_burned']
    for col in numeric_cols:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors='coerce')
        else:
            # Fallback default values if column missing
            default_val = {'age': 30, 'height_cm': 170, 'weight_kg': 70, 'daily_steps': 7000,
                           'heart_rate': 72, 'sleep_hours': 7.0, 'exercise_mins': 30,
                           'water_liters': 2.5, 'calories_burned': 2100}.get(col, 0)
            df[col] = default_val

    # Impute missing numeric values with medians
    for col in numeric_cols:
        if df[col].isnull().any():
            median_val = df[col].median()
            df[col] = df[col].fillna(median_val if not np.isnan(median_val) else 0)

    # Compute BMI if missing or incomplete
    if 'bmi' not in df.columns or df['bmi'].isnull().any():
        df['bmi'] = df.apply(lambda r: calculate_bmi(r['weight_kg'], r['height_cm']), axis=1)

    # Compute rule-based Fitness Score
    df['fitness_score'] = df.apply(
        lambda r: calculate_fitness_score(
            r['daily_steps'], r['exercise_mins'], r['sleep_hours'],
            r['water_liters'], r['heart_rate'], r['bmi']
        ),
        axis=1
    )

    # Target column validation / rule-based generation
    if TARGET_COLUMN not in df.columns or df[TARGET_COLUMN].isnull().all():
        df[TARGET_COLUMN] = df['fitness_score'].apply(score_to_category)
    else:
        df[TARGET_COLUMN] = df[TARGET_COLUMN].astype(str).str.strip().str.capitalize()
        # Fill missing targets
        df[TARGET_COLUMN] = df[TARGET_COLUMN].replace({'Nan': np.nan, '': np.nan})
        if df[TARGET_COLUMN].isnull().any():
            df[TARGET_COLUMN] = df.apply(
                lambda r: score_to_category(r['fitness_score']) if pd.isna(r[TARGET_COLUMN]) else r[TARGET_COLUMN],
                axis=1
            )

    return df
