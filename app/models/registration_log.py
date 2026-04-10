from datetime import datetime

from sqlalchemy import DateTime, Enum, ForeignKey, Integer, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base
from app.models.enums import RegistrationStatus


class RegistrationLog(Base):
    __tablename__ = "registration_logs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    registration_id: Mapped[int] = mapped_column(ForeignKey("registrations.id"), nullable=False)
    from_status: Mapped[RegistrationStatus | None] = mapped_column(Enum(RegistrationStatus), nullable=True)
    to_status: Mapped[RegistrationStatus] = mapped_column(Enum(RegistrationStatus), nullable=False)
    actor_id: Mapped[int | None] = mapped_column(ForeignKey("users.id"), nullable=True)
    reason: Mapped[str | None] = mapped_column(String(500), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    registration = relationship("Registration", back_populates="logs")
