import numpy as np
import xgboost as xgb
import json

# 1. Generate Synthetic Training Data for Monthly Recap Prediction
# Features: [food_ratio, weekend_ratio, velocity_idx, volatility_idx, recurring_ratio]
np.random.seed(42)
X = np.random.rand(200, 5).astype(np.float32)

# Target: 1 if user is predicted to overspend category next month, 0 otherwise
y = ((X[:, 0] * 0.4 + X[:, 1] * 0.3 + X[:, 3] * 0.3) > 0.5).astype(int)

# 2. Train XGBoost Model
model = xgb.XGBClassifier(n_estimators=10, max_depth=3, learning_rate=0.1)
model.fit(X, y)

print("XGBoost Model Trained Successfully.")

# 3. Export Trained XGBoost Model JSON format for mobile / embedded inference
model.save_model("mahari_recap.json")
print("Saved XGBoost model to mahari_recap.json")

# 4. Generate SHAP Explanation Template Mapping
shap_templates = {
    "feature_0": "Food & Dining spending drove {pct}% of this month's budget variation, mostly from weekend orders.",
    "feature_1": "Weekend activity accounted for {pct}% of your total non-discretionary spending spike.",
    "feature_2": "Higher transaction velocity in mid-month accelerated budget depletion.",
    "feature_3": "Spending volatility was {pct}% higher than your baseline average.",
    "feature_4": "Fixed recurring bills remained stable within expected bounds."
}

with open("shap_templates.json", "w") as f:
    json.dump(shap_templates, f, indent=2)

print("SHAP explanation templates exported successfully.")
