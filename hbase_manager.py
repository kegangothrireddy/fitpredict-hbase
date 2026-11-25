"""
hbase_manager.py
HBase connection, schema setup, CRUD, and scanning operations using HappyBase & Thrift.
Handles connection failures gracefully with clear reporting and offline fallback.
"""

import os
import time
import uuid
from datetime import datetime

# In-memory storage used ONLY when HBase is unavailable, with explicit indicator
_OFFLINE_STORAGE = []

class HBaseManager:
    def __init__(self, host=None, port=None, table_name="fitness_data"):
        # Configure HBase Thrift Host & Port via environment or default to localhost:9090
        self.host = host or os.environ.get("HBASE_HOST", "127.0.0.1")
        self.port = int(port or os.environ.get("HBASE_PORT", 9090))
        self.table_name = table_name
        self.column_families = {
            'profile': dict(),
            'activity': dict(),
            'health': dict(),
            'prediction': dict()
        }
        self.is_connected = False
        self.last_error = None
        self._test_and_initialize()

    def _get_connection(self):
        """
        Creates a HappyBase connection to the HBase Thrift server.
        """
        import happybase
        return happybase.Connection(host=self.host, port=self.port, timeout=5000)

    def _test_and_initialize(self):
        """
        Tests connection to HBase Thrift server and creates table if not existing.
        """
        try:
            connection = self._get_connection()
            connection.open()
            existing_tables = [t.decode('utf-8') if isinstance(t, bytes) else str(t) for t in connection.tables()]
            
            if self.table_name not in existing_tables:
                print(f"[*] HBase table '{self.table_name}' not found. Creating table with column families: {list(self.column_families.keys())}...")
                connection.create_table(self.table_name, self.column_families)
                print(f"[+] HBase table '{self.table_name}' created successfully.")
            
            connection.close()
            self.is_connected = True
            self.last_error = None
            print(f"[+] Connected to Apache HBase Thrift server at {self.host}:{self.port}")
        except Exception as e:
            self.is_connected = False
            self.last_error = str(e)
            print(f"[!] Warning: Apache HBase is not reachable at {self.host}:{self.port} - {e}")
            print("[!] The app will run in Offline/Demonstration mode. Records will be flagged as not stored in HBase.")

    def get_status(self):
        """
        Returns connection status dictionary.
        """
        # Re-check connection periodically if currently offline
        if not self.is_connected:
            self._test_and_initialize()

        return {
            "connected": self.is_connected,
            "host": self.host,
            "port": self.port,
            "table_name": self.table_name,
            "error": self.last_error if not self.is_connected else None
        }

    def save_prediction(self, prediction_result):
        """
        Saves a prediction record into HBase table under profile, activity, health, and prediction column families.
        Row Key Format: rec_{timestamp_ms}_{random_uuid} for chronological sorting and uniqueness.
        """
        status = self.get_status()
        now = datetime.now()
        timestamp_ms = int(time.time() * 1000)
        row_key = f"rec_{timestamp_ms}_{uuid.uuid4().hex[:6]}"
        date_str = now.strftime("%Y-%m-%d %H:%M:%S")

        inputs = prediction_result.get('inputs', {})
        level = prediction_result.get('predicted_level', 'Unknown')
        score = prediction_result.get('fitness_score', 0.0)
        bmi = prediction_result.get('bmi', 0.0)
        recs = " | ".join(prediction_result.get('recommendations', []))

        # Check if HBase is live
        if self.is_connected:
            try:
                connection = self._get_connection()
                connection.open()
                table = connection.table(self.table_name)

                # Prepare column data mapping with UTF-8 byte encoding
                data = {
                    b'profile:age': str(inputs.get('age', '')).encode('utf-8'),
                    b'profile:gender': str(inputs.get('gender', '')).encode('utf-8'),
                    b'profile:height_cm': str(inputs.get('height_cm', '')).encode('utf-8'),
                    b'profile:weight_kg': str(inputs.get('weight_kg', '')).encode('utf-8'),
                    b'profile:bmi': str(bmi).encode('utf-8'),

                    b'activity:daily_steps': str(inputs.get('daily_steps', '')).encode('utf-8'),
                    b'activity:exercise_mins': str(inputs.get('exercise_mins', '')).encode('utf-8'),
                    b'activity:calories_burned': str(inputs.get('calories_burned', '')).encode('utf-8'),

                    b'health:heart_rate': str(inputs.get('heart_rate', '')).encode('utf-8'),
                    b'health:sleep_hours': str(inputs.get('sleep_hours', '')).encode('utf-8'),
                    b'health:water_liters': str(inputs.get('water_liters', '')).encode('utf-8'),

                    b'prediction:fitness_score': str(score).encode('utf-8'),
                    b'prediction:predicted_level': str(level).encode('utf-8'),
                    b'prediction:recommendation': recs.encode('utf-8'),
                    b'prediction:prediction_date': date_str.encode('utf-8')
                }

                table.put(row_key.encode('utf-8'), data)
                connection.close()

                return {
                    "success": True,
                    "stored_in_hbase": True,
                    "row_key": row_key,
                    "message": f"Successfully committed record to HBase table '{self.table_name}' with row key: {row_key}"
                }
            except Exception as e:
                self.is_connected = False
                self.last_error = str(e)
                print(f"[!] HBase write error: {e}")

        # Fallback offline storage if HBase is unreachable
        record = {
            "row_key": row_key,
            "stored_in_hbase": False,
            "prediction_date": date_str,
            "age": inputs.get('age', ''),
            "gender": inputs.get('gender', ''),
            "height_cm": inputs.get('height_cm', ''),
            "weight_kg": inputs.get('weight_kg', ''),
            "bmi": bmi,
            "daily_steps": inputs.get('daily_steps', ''),
            "exercise_mins": inputs.get('exercise_mins', ''),
            "calories_burned": inputs.get('calories_burned', ''),
            "heart_rate": inputs.get('heart_rate', ''),
            "sleep_hours": inputs.get('sleep_hours', ''),
            "water_liters": inputs.get('water_liters', ''),
            "fitness_score": score,
            "predicted_level": level,
            "recommendation": recs
        }
        _OFFLINE_STORAGE.insert(0, record)

        return {
            "success": True,
            "stored_in_hbase": False,
            "row_key": row_key,
            "message": "Saved to temporary local memory only. HBase Thrift server is currently unreachable."
        }

    def get_all_predictions(self, limit=50):
        """
        Retrieves recent prediction records from HBase table (or fallback if offline).
        Returns list of standardized record dictionaries.
        """
        records = []
        if self.is_connected:
            try:
                connection = self._get_connection()
                connection.open()
                table = connection.table(self.table_name)

                # Scan table in reverse or get rows
                for row_key, data in table.scan(limit=limit, reverse=True):
                    record = {
                        "row_key": row_key.decode('utf-8', errors='ignore'),
                        "stored_in_hbase": True,
                        "age": data.get(b'profile:age', b'').decode('utf-8', errors='ignore'),
                        "gender": data.get(b'profile:gender', b'').decode('utf-8', errors='ignore'),
                        "height_cm": data.get(b'profile:height_cm', b'').decode('utf-8', errors='ignore'),
                        "weight_kg": data.get(b'profile:weight_kg', b'').decode('utf-8', errors='ignore'),
                        "bmi": data.get(b'profile:bmi', b'').decode('utf-8', errors='ignore'),
                        "daily_steps": data.get(b'activity:daily_steps', b'').decode('utf-8', errors='ignore'),
                        "exercise_mins": data.get(b'activity:exercise_mins', b'').decode('utf-8', errors='ignore'),
                        "calories_burned": data.get(b'activity:calories_burned', b'').decode('utf-8', errors='ignore'),
                        "heart_rate": data.get(b'health:heart_rate', b'').decode('utf-8', errors='ignore'),
                        "sleep_hours": data.get(b'health:sleep_hours', b'').decode('utf-8', errors='ignore'),
                        "water_liters": data.get(b'health:water_liters', b'').decode('utf-8', errors='ignore'),
                        "fitness_score": data.get(b'prediction:fitness_score', b'').decode('utf-8', errors='ignore'),
                        "predicted_level": data.get(b'prediction:predicted_level', b'').decode('utf-8', errors='ignore'),
                        "recommendation": data.get(b'prediction:recommendation', b'').decode('utf-8', errors='ignore'),
                        "prediction_date": data.get(b'prediction:prediction_date', b'').decode('utf-8', errors='ignore')
                    }
                    records.append(record)
                connection.close()
                return records
            except Exception as e:
                self.is_connected = False
                self.last_error = str(e)
                print(f"[!] Error reading from HBase: {e}")

        # Return fallback records
        return _OFFLINE_STORAGE[:limit]
