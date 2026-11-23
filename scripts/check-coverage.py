import sys
import xml.etree.ElementTree as ET

report = ET.parse(sys.argv[1]).getroot()
for metric in ("LINE", "BRANCH"):
    counter = report.find(f"counter[@type='{metric}']")
    if counter is None:
        raise SystemExit(f"Отсутствует метрика {metric}")
    covered = int(counter.attrib["covered"])
    total = covered + int(counter.attrib["missed"])
    ratio = covered / total if total else 1
    print(f"{metric}: {ratio:.1%}")
    if ratio < 0.8:
        raise SystemExit(f"Покрытие {metric} ниже 80%")
