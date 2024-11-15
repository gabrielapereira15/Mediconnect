package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Doctor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


public class AppointmentMock implements AppointmentClient {

    private final ObjectMapper objectMapper;
    Logger logger = Logger.getLogger(AppointmentMock.class.getName());

    public AppointmentMock() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public List<Doctor> getDoctors() {
        var response = """
                {
                  "doctors": [
                    {
                      "id": 1,
                      "name": "Roberto",
                      "specialty": "Dentist",
                      "photo": "https://mediconnect.ca/{cod-clinic}/doc-photo/1"
                    },
                    {
                      "id": 2,
                      "name": "Gino",
                      "specialty": "Cardiologist",
                      "photo": "https://mediconnect.ca/{cod-clinic}/doctor/1/photo"
                    },
                    {
                      "id": 3,
                      "name": "Gabriela",
                      "specialty": "Gynecologist",
                      "photo": "https://mediconnect.ca/{cod-clinic}/doc-photo/1"
                    },
                    {
                      "id": 4,
                      "name": "Eduardo",
                      "specialty": "Chiropractic",
                      "photo": "https://mediconnect.ca/{cod-clinic}/doc-photo/1"
                    }
                  ]
                }
                        """;
        try {
            return objectMapper.readValue(response, DoctorsResponse.class).getDoctors();
        } catch (IOException e) {
            logger.warning("Error parsing response get doctors");
            return Collections.emptyList();
        }
    }

    @Override
    public Doctor getDoctor(String doctorId) {
        var response = """
                {
                  "id": 1,
                  "name": "Roberto",
                  "score": 4.5,
                  "experienceYears": "10 years",
                  "description": "Dr. Rezwana Afrin was born in 1978 in Karachi, Pakistan. She completed her undergraduate studies at Karachi University and earned her medical degree...",
                  "photo": "https://mediconnect.ca/{cod-clinic}/doctor/1/photo",
                  "schedule": [
                    {
                      "date": "Fri, 9 Jun",
                      "times": ["11 AM", "13 PM", "15 PM"]
                    },
                    {
                      "date": "Fri, 10 Jun",
                      "times": ["10 AM", "11 PM", "12 PM", "13 PM", "14 PM", "15 PM"]
                    },
                    {
                      "date": "Fri, 11 Jun",
                      "times": ["11 AM", "13 PM", "15 PM"]
                    },
                                        {
                      "date": "Fri, 12 Jun",
                      "times": ["11 AM", "13 PM", "15 PM", "17 PM"]
                    }
                  ]
                }
                """;
        try {
            return objectMapper.readValue(response, Doctor.class);
        } catch (IOException e) {
            throw new RuntimeException("Error to parse response get doctor");
        }
    }
}
