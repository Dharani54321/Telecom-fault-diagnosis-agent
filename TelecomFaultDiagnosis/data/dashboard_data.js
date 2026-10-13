window.NOC_DATA = {
  "topology": {
    "towers": [
      {
        "id": "T1",
        "name": "Central Hub",
        "type": "Core",
        "x": 90,
        "y": 200
      },
      {
        "id": "T2",
        "name": "North Junction",
        "type": "Aggregation",
        "x": 250,
        "y": 200
      },
      {
        "id": "T3",
        "name": "East Ridge",
        "type": "Aggregation",
        "x": 410,
        "y": 90
      },
      {
        "id": "T4",
        "name": "Metro Hub",
        "type": "Aggregation",
        "x": 410,
        "y": 310
      },
      {
        "id": "T5",
        "name": "Hill Top",
        "type": "Access",
        "x": 570,
        "y": 60
      },
      {
        "id": "T6",
        "name": "Harbor Point",
        "type": "Access",
        "x": 570,
        "y": 250
      },
      {
        "id": "T7",
        "name": "University",
        "type": "Access",
        "x": 570,
        "y": 150
      },
      {
        "id": "T8",
        "name": "Airport",
        "type": "Access",
        "x": 730,
        "y": 200
      },
      {
        "id": "T9",
        "name": "Remote Village",
        "type": "Access",
        "x": 730,
        "y": 340
      }
    ],
    "links": [
      {
        "from": "T1",
        "to": "T2",
        "type": "Fiber"
      },
      {
        "from": "T2",
        "to": "T3",
        "type": "Fiber"
      },
      {
        "from": "T2",
        "to": "T4",
        "type": "Fiber"
      },
      {
        "from": "T3",
        "to": "T5",
        "type": "Microwave"
      },
      {
        "from": "T3",
        "to": "T7",
        "type": "Fiber"
      },
      {
        "from": "T4",
        "to": "T6",
        "type": "Fiber"
      },
      {
        "from": "T5",
        "to": "T6",
        "type": "Microwave"
      },
      {
        "from": "T6",
        "to": "T8",
        "type": "Fiber"
      },
      {
        "from": "T7",
        "to": "T8",
        "type": "Microwave"
      }
    ]
  },
  "runs": [
    {
      "id": "LOG-001",
      "timestamp": "2026-09-30 03:47:38",
      "source": "T1",
      "destination": "T6",
      "bfsPath": [
        "T1",
        "T2",
        "T4",
        "T6"
      ],
      "dfsPath": [
        "T1",
        "T2",
        "T3",
        "T5",
        "T6"
      ],
      "reroutePath": [
        "T1",
        "T2",
        "T3",
        "T5",
        "T6"
      ],
      "affected": [],
      "fault": {
        "type": "Link Failure",
        "location": "T2-T4"
      },
      "symptoms": [
        "Link down",
        "Connection lost",
        "Packet loss on T2-T4"
      ],
      "diagnosis": {
        "log_id": "LOG-001",
        "fault_type": "Link Failure",
        "confidence": 100,
        "matched_rule": "link down + connection lost",
        "matched_symptoms": [
          "link down",
          "connection lost"
        ],
        "severity": "HIGH",
        "reason": "Link Failure symptoms detected (link down + connection lost)",
        "recommended_action": "Inspect the affected network link (fiber/microwave), check connectors and switch over to the backup route.",
        "all_scores": {
          "Tower Failure": 0,
          "Link Failure": 100,
          "Congestion": 0,
          "Signal/Power Issue": 0,
          "Hardware Failure": 0
        }
      },
      "correct": true
    },
    {
      "id": "LOG-002",
      "timestamp": "2026-09-30 03:47:38",
      "source": "T1",
      "destination": "T6",
      "bfsPath": [
        "T1",
        "T2",
        "T4",
        "T6"
      ],
      "dfsPath": [
        "T1",
        "T2",
        "T3",
        "T5",
        "T6"
      ],
      "reroutePath": [
        "T1",
        "T2",
        "T3",
        "T5",
        "T6"
      ],
      "affected": [
        "T4"
      ],
      "fault": {
        "type": "Tower Failure",
        "location": "T4"
      },
      "symptoms": [
        "No signal",
        "Tower unreachable",
        "Heartbeat timeout from T4"
      ],
      "diagnosis": {
        "log_id": "LOG-002",
        "fault_type": "Tower Failure",
        "confidence": 100,
        "matched_rule": "no signal + tower unreachable",
        "matched_symptoms": [
          "no signal",
          "tower unreachable"
        ],
        "severity": "CRITICAL",
        "reason": "Tower Failure symptoms detected (no signal + tower unreachable)",
        "recommended_action": "Dispatch a field team to the tower; check the site power, RF unit and backhaul, then reboot the base station.",
        "all_scores": {
          "Tower Failure": 100,
          "Link Failure": 0,
          "Congestion": 0,
          "Signal/Power Issue": 0,
          "Hardware Failure": 0
        }
      },
      "correct": true
    },
    {
      "id": "LOG-003",
      "timestamp": "2026-09-30 03:47:38",
      "source": "T1",
      "destination": "T6",
      "bfsPath": [
        "T1",
        "T2",
        "T4",
        "T6"
      ],
      "dfsPath": [
        "T1",
        "T2",
        "T3",
        "T5",
        "T6"
      ],
      "reroutePath": [
        "T1",
        "T2",
        "T3",
        "T5",
        "T6"
      ],
      "affected": [
        "T4"
      ],
      "fault": {
        "type": "Signal/Power Issue",
        "location": "T4"
      },
      "symptoms": [
        "Power low",
        "Equipment offline",
        "Battery backup draining at T4"
      ],
      "diagnosis": {
        "log_id": "LOG-003",
        "fault_type": "Signal/Power Issue",
        "confidence": 100,
        "matched_rule": "power low + equipment offline",
        "matched_symptoms": [
          "power low",
          "equipment offline"
        ],
        "severity": "HIGH",
        "reason": "Signal/Power Issue symptoms detected (power low + equipment offline)",
        "recommended_action": "Check mains supply, rectifier and battery backup; restore power and bring the equipment back online.",
        "all_scores": {
          "Tower Failure": 0,
          "Link Failure": 0,
          "Congestion": 0,
          "Signal/Power Issue": 100,
          "Hardware Failure": 0
        }
      },
      "correct": true
    },
    {
      "id": "LOG-004",
      "timestamp": "2026-09-30 03:47:38",
      "source": "T1",
      "destination": "T6",
      "bfsPath": [
        "T1",
        "T2",
        "T4",
        "T6"
      ],
      "dfsPath": [
        "T1",
        "T2",
        "T3",
        "T5",
        "T6"
      ],
      "reroutePath": [
        "T1",
        "T2",
        "T3",
        "T5",
        "T6"
      ],
      "affected": [
        "T4"
      ],
      "fault": {
        "type": "Hardware Failure",
        "location": "T4"
      },
      "symptoms": [
        "Device error",
        "Hardware alarm",
        "Line card fault at T4"
      ],
      "diagnosis": {
        "log_id": "LOG-004",
        "fault_type": "Hardware Failure",
        "confidence": 100,
        "matched_rule": "device error + hardware alarm",
        "matched_symptoms": [
          "device error",
          "hardware alarm"
        ],
        "severity": "HIGH",
        "reason": "Hardware Failure symptoms detected (device error + hardware alarm)",
        "recommended_action": "Replace or reseat the faulty card/module and raise a hardware maintenance ticket.",
        "all_scores": {
          "Tower Failure": 0,
          "Link Failure": 0,
          "Congestion": 0,
          "Signal/Power Issue": 0,
          "Hardware Failure": 100
        }
      },
      "correct": true
    },
    {
      "id": "LOG-005",
      "timestamp": "2026-09-30 03:47:38",
      "source": "T1",
      "destination": "T6",
      "bfsPath": [
        "T1",
        "T2",
        "T4",
        "T6"
      ],
      "dfsPath": [
        "T1",
        "T2",
        "T3",
        "T5",
        "T6"
      ],
      "reroutePath": [
        "T1",
        "T2",
        "T4",
        "T6"
      ],
      "affected": [],
      "fault": {
        "type": "Congestion",
        "location": "T4"
      },
      "symptoms": [
        "High traffic",
        "Slow network",
        "Utilisation above 90% at T4"
      ],
      "diagnosis": {
        "log_id": "LOG-005",
        "fault_type": "Congestion",
        "confidence": 100,
        "matched_rule": "high traffic + slow network",
        "matched_symptoms": [
          "high traffic",
          "slow network"
        ],
        "severity": "MEDIUM",
        "reason": "Congestion symptoms detected (high traffic + slow network)",
        "recommended_action": "Rebalance traffic, enable load-balancing/QoS and plan extra capacity on the busy link.",
        "all_scores": {
          "Tower Failure": 0,
          "Link Failure": 0,
          "Congestion": 100,
          "Signal/Power Issue": 0,
          "Hardware Failure": 0
        }
      },
      "correct": true
    }
  ]
};
