"""Extract text from a PDF page by page. Usage: python -I pdf2txt.py paper.pdf > paper.txt"""
import sys
import pypdf

r = pypdf.PdfReader(sys.argv[1])
for i, p in enumerate(r.pages, 1):
    print(f"\n=== PAGE {i} ===\n" + (p.extract_text() or ""))
