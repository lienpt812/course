"""Shared pytest fixtures for backend tests.

Adjust database/session fixtures here when test infrastructure is ready.
"""

import pytest


@pytest.fixture
def sample_data() -> dict:
    """Basic fixture used by starter tests."""
    return {"ok": True}
