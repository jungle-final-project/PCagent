import unittest
import sys
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

import reranker_service


class FakeCursor:
    def __init__(self, row):
        self.row = row
        self.statements = []
        self.params = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params=None):
        self.statements.append(statement)
        self.params.append(params)

    def fetchone(self):
        return self.row


class FakeConnection:
    def __init__(self, cursor):
        self.cursor_instance = cursor

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def cursor(self):
        return self.cursor_instance


class RerankerTrainingWorkerTest(unittest.TestCase):
    def test_process_one_training_job_skips_polling_when_training_schema_is_missing(self):
        cursor = FakeCursor({"ready": False})

        with patch.object(reranker_service, "db_connection", return_value=FakeConnection(cursor)):
            processed = reranker_service.process_one_training_job("xgb-reranker-test", 50)

        self.assertFalse(processed)
        self.assertEqual(1, len(cursor.statements))
        self.assertNotIn("UPDATE recommendation_training_jobs", cursor.statements[0])


if __name__ == "__main__":
    unittest.main()
