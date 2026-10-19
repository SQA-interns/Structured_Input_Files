import type { FormConfig } from "./types";

/** Form configuration used by the unit tests. */
export const testConfig: FormConfig = {
  conferenceName: "Konferenca",
  options: [
    { id: "ws-a", name: "Workshop A", category: "WORKSHOP", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "oth-t", name: "Tour", category: "OTHER", availableTo: ["EXTERNAL"] },
    { id: "meal-l", name: "Lunch", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "oth-m", name: "Mentoring", category: "OTHER", availableTo: ["STUDENT"] },
    { id: "ev-s", name: "Student party", category: "EVENT", availableTo: ["STUDENT"] },
  ],
  consents: [
    { id: "privacy", text: "I agree to the processing.", mandatory: true },
    { id: "photos", text: "Photos are fine.", mandatory: false },
  ],
  recaptcha: { siteKey: "", testMode: true },
};
