import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CSV_PATH = os.path.normpath(os.path.join(BASE_DIR, "../data/ocr_paragraphs.csv"))

def _have_sim_engine() -> bool:
    try:
        from workbook.run_workbook_simulation import run_workbook_simulation  # noqa: F401
        return True
    except Exception:
        return False

HAVE_SIM_ENGINE = _have_sim_engine()


