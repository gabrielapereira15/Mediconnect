package com.example.mediconnect_android.client;

import com.example.mediconnect_android.client.response.DoctorsResponse;
import com.example.mediconnect_android.model.Appointment;
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
    public List<Appointment> getAppointments(String email) {
        return Collections.emptyList();
    }

    @Override
    public Boolean createAppointment(String appointmentJson) {
        return null;
    }

    @Override
    public Boolean cancelAppointment(String appointmentId) {
        return null;
    }

}
