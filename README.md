# An Apache HBase-Based Predictive Analytics System for Fitness and Wellness

A full-stack Big Data and Machine Learning web application designed to analyze personal biometric data, predict fitness categories using a **Scikit-learn Random Forest Classifier**, compute transparent **BMI** and **Fitness Scores (0-100)**, provide **personalized wellness recommendations**, and persist all records into **Apache HBase** through **HappyBase** and the **HBase Thrift Gateway**.

---

## 1. System Architecture

```text
       [ User Web Browser ]
                │
                ▼  HTTP Requests
       [ Flask Application (main.py) ]
          │                    │
          ▼                    ▼
[ Scikit-Learn Model ]   [ HappyBase Client ]
 (Random Forest .pkl)          │  Thrift RPC (Port 9090)
                               ▼
                    [ Apache HBase (WSL/Linux) ]
                     Table: 'fitness_data'
                     ├─ Column Family: profile: (age, gender, height_cm, weight_kg, bmi)
                     ├─ Column Family: activity: (daily_steps, exercise_mins, calories_burned)
                     ├─ Column Family: health: (heart_rate, sleep_hours, water_liters)
                     └─ Column Family: prediction: (fitness_score, predicted_level, recommendation, date)
```

---

## 2. Technology Stack

- **Backend Framework:** Python 3.10+ / Flask 3.0
- **Big Data NoSQL Storage:** Apache HBase (Standalone / Pseudo-distributed) via HappyBase & Thrift
- **Machine Learning:** Scikit-learn Random Forest Classifier (Multi-class: Low, Moderate, High)
- **Data Engineering:** Pandas & NumPy
- **Frontend:** HTML5, CSS3, JavaScript (Bootstrap 5, Chart.js, FontAwesome 6)
- **Development Environment:** VS Code on Windows 10/11 with HBase running in WSL2 Ubuntu

---

## 3. Project Directory Structure

```text
hbase-fitness-analytics/
├── fitness_data.csv            # Training & cohort dataset
├── requirements.txt            # Python package dependencies
├── data_processor.py           # Preprocessing, BMI & heuristic scoring rules
├── model.py                    # Random Forest training & inference pipeline
├── hbase_manager.py            # HappyBase client, table management & CRUD
├── main.py                     # Flask web server & REST endpoints
├── fitness_model.pkl           # Saved model weights (generated on first run)
├── static/
│   ├── css/
│   │   └── style.css           # Custom UI styling
│   └── js/
│       └── dashboard.js        # Chart.js visualization logic
├── templates/
│   ├── base.html               # Master layout with HBase health badge
│   ├── index.html              # KPI cards, distribution charts, recent runs
│   ├── predict.html            # Input form with live BMI preview
│   ├── result.html             # Prediction outcome, score breakdown, HBase confirmation
│   ├── history.html            # Full tabular scan of HBase table
│   └── data.html               # CSV dataset explorer
└── README.md                   # Complete documentation & setup instructions
```

---

## 4. Step-by-Step Installation & Execution Guide

### Step 1: Open VS Code & Create Project Folder
1. Create a directory named `hbase-fitness-analytics` on your system.
2. Open the folder in **VS Code**.
3. Create a Python Virtual Environment:
   ```bash
   python -m venv venv
   # On Windows:
   venv\Scripts\activate
   # On macOS/Linux/WSL:
   source venv/bin/activate
   ```

### Step 2: Install Python Dependencies
```bash
pip install -r requirements.txt
```

### Step 3: Inspect Dataset & Train Machine Learning Model
Train the Random Forest model and generate `fitness_model.pkl`:
```bash
python model.py
```
*Output:*
```text
[*] Loading and preprocessing data from fitness_data.csv...
[*] Training Random Forest Classifier on 40 samples...
[+] Model Training Complete! Accuracy: 91.67%
[+] Saved trained model bundle to 'fitness_model.pkl'
```

---

## 5. Apache HBase & Thrift Server Setup in WSL (Ubuntu)

If running on Windows, Apache HBase runs reliably inside **WSL2 (Windows Subsystem for Linux)**.

### A. Install Java 8 / 11 in WSL
```bash
sudo apt update
sudo apt install openjdk-11-jdk -y
java -version
```

### B. Download & Extract Apache HBase
```bash
cd ~
wget https://archive.apache.org/dist/hbase/2.4.17/hbase-2.4.17-bin.tar.gz
tar -xzvf hbase-2.4.17-bin.tar.gz
cd hbase-2.4.17
```

### C. Set JAVA_HOME in `conf/hbase-env.sh`
Open `conf/hbase-env.sh` and set:
```bash
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

### D. Start HBase in Standalone Mode
```bash
bin/start-hbase.sh
```

### E. Start HBase Thrift Gateway (Crucial for Python HappyBase)
The Thrift server listens on port **9090** to allow Python clients to execute HBase commands:
```bash
bin/hbase-daemon.sh start thrift -p 9090
```
Verify the Thrift port is active:
```bash
netstat -tuln | grep 9090
```

### F. Create the Table in HBase Shell (Optional)
The application automatically creates the table upon startup if it does not already exist. To create it manually in the HBase shell:
```bash
bin/hbase shell
```
In the prompt:
```ruby
create 'fitness_data', 'profile', 'activity', 'health', 'prediction'
list
describe 'fitness_data'
exit
```

---

## 6. Run the Flask Web Application

Once your virtual environment is active (and HBase Thrift is running in WSL or Linux):
```bash
python main.py
```

Open your browser and navigate to:
```text
http://127.0.0.1:5000
```

### If Running Flask on Windows with HBase in WSL:
By default, WSL2 localhost ports are mirrored to Windows `127.0.0.1`. If your WSL IP differs, set the environment variable:
```bash
set HBASE_HOST=172.x.x.x
python main.py
```

---

## 7. Verifying Big Data Storage in HBase

1. Go to `http://127.0.0.1:5000/predict`.
2. Fill out biometric attributes (Age, Gender, Height, Weight, Steps, Sleep, etc.) and submit.
3. Observe the green confirmation banner:
   - **Row Key:** `rec_1728839201948_e92ab1`
   - **Status:** Archived to Apache HBase table `fitness_data`.
4. Verify from the HBase Shell in your terminal:
   ```bash
   bin/hbase shell
   scan 'fitness_data', {LIMIT => 5}
   ```
   You will see row records stored across column families `profile:`, `activity:`, `health:`, and `prediction:`.

---

## 8. Common Troubleshooting & Solutions

| Issue | Root Cause | Solution |
| :--- | :--- | :--- |
| `TTransportException: Could not connect to 127.0.0.1:9090` | HBase Thrift server is not started | Run `bin/hbase-daemon.sh start thrift -p 9090` in your HBase directory. |
| `App loads but says HBase Thrift: Offline` | Thrift service port unreachable | The application automatically falls back to in-memory mode without crashing. Start Thrift and refresh the page. |
| `FileNotFoundError: fitness_data.csv` | Working directory mismatch | Ensure you run the commands from the root directory where `fitness_data.csv` is located. |
| `scikit-learn version mismatch on pkl load` | Different Python environments | Run `python model.py` to regenerate `fitness_model.pkl` in your active environment. |
