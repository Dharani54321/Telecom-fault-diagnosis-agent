"""Run with:  python python/test_classifier.py"""
import os, sys, unittest
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from fault_classifier import classify


class ClassifierTests(unittest.TestCase):
    def check(self, symptoms, expected, conf=100):
        r = classify(symptoms)
        self.assertEqual(r["fault_type"], expected)
        self.assertEqual(r["confidence"], conf)

    def test_tower(self):      self.check(["No signal", "Tower unreachable"], "Tower Failure")
    def test_link(self):       self.check(["Link down", "Connection lost"], "Link Failure")
    def test_congestion(self): self.check(["High traffic", "Slow network"], "Congestion")
    def test_power(self):      self.check(["Power low", "Equipment offline"], "Signal/Power Issue")
    def test_hardware(self):   self.check(["Device error", "Hardware alarm"], "Hardware Failure")
    def test_partial(self):    self.check(["No signal"], "Tower Failure", 50)
    def test_unknown(self):    self.check(["Fan noise"], "Unknown Fault", 0)
    def test_case_and_noise(self):
        self.check(["LINK DOWN on T2-T4", "connection lost", "Packet loss"], "Link Failure")
    def test_empty(self):      self.check([], "Unknown Fault", 0)


if __name__ == "__main__":
    unittest.main()
