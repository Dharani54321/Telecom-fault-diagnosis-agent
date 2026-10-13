"""
Fault classifier - the "AI-style" (rule-based) part of the Telecom Fault Diagnosis Agent.

Usage:  python fault_classifier.py [symptoms.json] [diagnosis.json]
Reads   data/symptoms.json   (written by Java)
Writes  data/diagnosis.json  (read back by Java)

Each rule = a fault type + the symptom phrases that identify it.
Confidence = matched phrases / required phrases  (2 of 2 = 100 %, 1 of 2 = 50 %).
Only the standard library is used.
"""
import json
import os
import sys

# Order matters only when two rules score the same: the earlier rule wins.
RULES = [
    {"fault": "Tower Failure", "keywords": ["no signal", "tower unreachable"], "severity": "CRITICAL",
     "action": "Dispatch a field team to the tower; check the site power, RF unit and backhaul, then reboot the base station."},
    {"fault": "Link Failure", "keywords": ["link down", "connection lost"], "severity": "HIGH",
     "action": "Inspect the affected network link (fiber/microwave), check connectors and switch over to the backup route."},
    {"fault": "Congestion", "keywords": ["high traffic", "slow network"], "severity": "MEDIUM",
     "action": "Rebalance traffic, enable load-balancing/QoS and plan extra capacity on the busy link."},
    {"fault": "Signal/Power Issue", "keywords": ["power low", "equipment offline"], "severity": "HIGH",
     "action": "Check mains supply, rectifier and battery backup; restore power and bring the equipment back online."},
    {"fault": "Hardware Failure", "keywords": ["device error", "hardware alarm"], "severity": "HIGH",
     "action": "Replace or reseat the faulty card/module and raise a hardware maintenance ticket."},
]


def classify(symptoms):
    """Return the diagnosis dictionary for a list of symptom strings."""
    text = [s.lower() for s in symptoms]
    scores, matches = {}, {}
    for rule in RULES:
        hit = [k for k in rule["keywords"] if any(k in line for line in text)]
        matches[rule["fault"]] = hit
        scores[rule["fault"]] = round(100 * len(hit) / len(rule["keywords"]))

    best = max(RULES, key=lambda r: scores[r["fault"]])   # max() keeps the first rule on ties
    conf = scores[best["fault"]]

    if conf == 0:
        return {"fault_type": "Unknown Fault", "confidence": 0, "matched_rule": "none",
                "matched_symptoms": [], "severity": "LOW",
                "reason": "No known symptom pattern was found in the log.",
                "recommended_action": "Escalate to a senior NOC engineer for manual log analysis.",
                "all_scores": scores}

    hit = matches[best["fault"]]
    if conf == 100:
        reason = "%s symptoms detected (%s)" % (best["fault"], " + ".join(hit))
    else:
        reason = ("Partial match: only '%s' seen, expected %s. Verify manually before acting."
                  % (hit[0], " + ".join(best["keywords"])))
    return {"fault_type": best["fault"], "confidence": conf,
            "matched_rule": " + ".join(best["keywords"]), "matched_symptoms": hit,
            "severity": best["severity"], "reason": reason,
            "recommended_action": best["action"], "all_scores": scores}


def main():
    base = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data")
    src = sys.argv[1] if len(sys.argv) > 1 else os.path.join(base, "symptoms.json")
    dst = sys.argv[2] if len(sys.argv) > 2 else os.path.join(base, "diagnosis.json")
    try:
        with open(src, encoding="utf-8") as f:
            log = json.load(f)
    except FileNotFoundError:
        sys.exit("ERROR: symptom file not found: " + src)
    except json.JSONDecodeError as e:
        sys.exit("ERROR: symptom file is not valid JSON: %s" % e)

    result = {"log_id": log.get("log_id", "N/A")}
    result.update(classify(log.get("symptoms", [])))
    with open(dst, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    print("Diagnosis written to " + dst + ": " + result["fault_type"] + " (" + str(result["confidence"]) + "%)")


if __name__ == "__main__":
    main()
