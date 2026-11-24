"""List all works citing a paper via OpenAlex (no API key needed).

Usage: python3 openalex_citers.py <arXiv-ID | DOI | OpenAlex W-id> [--tsv]
Prints: year, title, DOI/arXiv id, OpenAlex id. Semantic Scholar often rate-limits; OpenAlex rarely does.
"""
import json, sys, urllib.parse, urllib.request

API = "https://api.openalex.org/works"


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "verified-claim-research"}), timeout=30) as r:
        return json.load(r)


def resolve(ref):
    if ref.startswith("W"):
        return ref
    doi = ref if ref.startswith("10.") else f"10.48550/arxiv.{ref}"
    res = get(f"{API}?filter=doi:{urllib.parse.quote(doi)}")["results"]
    if not res:
        sys.exit(f"not found in OpenAlex: {ref}")
    return res[0]["id"].rsplit("/", 1)[-1]


def main():
    ref, tsv = sys.argv[1], "--tsv" in sys.argv
    wid = resolve(ref)
    cursor, rows = "*", []
    while cursor:
        page = get(f"{API}?filter=cites:{wid}&per-page=200&cursor={cursor}&select=id,title,publication_year,doi,ids")
        rows += page["results"]
        cursor = page["meta"].get("next_cursor")
    print(f"# {len(rows)} works citing {ref} ({wid})")
    for w in sorted(rows, key=lambda w: -(w.get("publication_year") or 0)):
        doi = (w.get("doi") or "").replace("https://doi.org/", "")
        line = [str(w.get("publication_year")), w.get("title") or "", doi, w["id"].rsplit("/", 1)[-1]]
        print("\t".join(line) if tsv else " | ".join(line))


if __name__ == "__main__":
    main()
