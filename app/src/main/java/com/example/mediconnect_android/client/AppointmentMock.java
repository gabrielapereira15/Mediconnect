package com.example.mediconnect_android.client;

import com.example.mediconnect_android.model.Doctor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


public class AppointmentMock implements AppointmentClient {

    Logger logger = Logger.getLogger(AppointmentMock.class.getName());
    private final ObjectMapper objectMapper;

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
  "description": "Loren ipso",
  "photo": "https://mediconnect.ca/{cod-clinic}/doctor/1/photo",
  "schedule": [
    {
      "date": "11/12/2024",
      "times": ["11 am", "13 pm", "15 pm"]
    },
    {
      "date": "12/12/2024",
      "times": ["10 am", "11 pm", "12 pm", "13 pm", "14 pm", "15 pm"]
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
