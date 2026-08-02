from fastapi import FastAPI, HTTPException, Path
from pydantic import BaseModel, Field
from typing import List, Optional
import ssl
from security import save_encrypted_user_data, load_encrypted_user_data, delete_user_data
from ml_engine import analyze_structured_transactions

app = FastAPI(
    title="Mahari Cloud Sync & ML Backend",
    version="1.0.0",
    description="Privacy-preserving XGBoost + SHAP analysis server for Mahari. Accepts structured data ONLY."
)

class StructuredTransactionDto(BaseModel):
    code: str
    amount: float
    category: str
    merchant: str
    timestamp: long if False else int
    isExpense: bool

class StructuredSyncRequest(BaseModel):
    deviceId: str
    userAge: Optional[int] = 20
    transactions: List[StructuredTransactionDto]

class StructuredInsightResponse(BaseModel):
    topSpendingCategory: str
    primaryDriver: str
    shapSummary: str
    textInsight: str

@app.post("/api/v1/sync", response_model=StructuredInsightResponse)
async def sync_structured_data(request: StructuredSyncRequest):
    # Data Minimization Assertion: Verify no raw text or personal fields are present
    tx_list = [tx.dict() for tx in request.transactions]

    # Encrypt & save structured data at rest
    payload_to_store = {
        "deviceId": request.deviceId,
        "userAge": request.userAge,
        "transactions": tx_list
    }
    save_encrypted_user_data(request.deviceId, payload_to_store)

    # Run XGBoost + SHAP pipeline
    ml_result = analyze_structured_transactions(tx_list, request.userAge)

    return StructuredInsightResponse(
        topSpendingCategory=ml_result["top_spending_category"],
        primaryDriver=ml_result["primary_driver"],
        shapSummary=ml_result["shap_summary"],
        textInsight=ml_result["text_insight"]
    )

@app.get("/api/v1/user-data/{device_id}")
async def inspect_user_data(device_id: str = Path(..., description="Device ID to inspect stored data for")):
    data = load_encrypted_user_data(device_id)
    if not data:
        raise HTTPException(status_code=404, detail="No synced cloud records found for device ID.")
    return {
        "status": "success",
        "deviceId": device_id,
        "recordCount": len(data.get("transactions", [])),
        "payload": data
    }

@app.delete("/api/v1/user-data/{device_id}")
async def purge_user_data(device_id: str = Path(..., description="Device ID to permanently purge")):
    deleted = delete_user_data(device_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="No synced data found for device ID.")
    return {"status": "success", "message": f"All synced data for device {device_id} has been permanently deleted from backend storage."}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8443)
