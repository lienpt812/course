from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Course Registration System"
    api_v1_prefix: str = "/api/v1"
    secret_key: str = "change-me-in-production"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 15
    refresh_token_expire_days: int = 7

    postgres_server: str = "localhost"
    postgres_user: str = "postgres"
    postgres_password: str = "postgres"
    postgres_db: str = "course_db"
    postgres_port: int = 5432
    postgres_host_port: int = 5432  # Host machine port for Docker mapping

    cors_origins: str = "*"

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg2://{self.postgres_user}:{self.postgres_password}"
            f"@{self.postgres_server}:{self.postgres_port}/{self.postgres_db}"
        )

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )


settings = Settings()
