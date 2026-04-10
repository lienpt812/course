from datetime import datetime

from pydantic import BaseModel


class CertificateItem(BaseModel):
    id: int
    user_id: int
    course_id: int
    issued_at: datetime
    verification_code: str
    pdf_url: str | None = None

    class Config:
        from_attributes = True
