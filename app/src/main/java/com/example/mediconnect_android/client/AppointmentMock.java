package com.example.mediconnect_android.client;

import com.example.mediconnect_android.client.response.AppointmentsResponse;
import com.example.mediconnect_android.client.response.DoctorsResponse;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Schedule;
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

    @Override
    public List<Appointment> getAppointments(int patientId) {
        var response = """
                {
                  "appointments": [
                    {
                      "appointmentId": 1,
                      "status": "Scheduled",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-16",
                        "time": "10:00 AM"
                      },
                      "isVirtual": true
                    },
                    {
                      "appointmentId": 2,
                      "status": "Completed",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-17",
                        "time": "11:00 AM"
                      },
                      "isVirtual": false
                    },
                    {
                      "appointmentId": 3,
                      "status": "Scheduled",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-18",
                        "time": "09:00 AM"
                      },
                      "isVirtual": true
                    },
                    {
                      "appointmentId": 4,
                      "status": "Scheduled",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-19",
                        "time": "02:00 PM"
                      },
                      "isVirtual": false
                    },
                    {
                      "appointmentId": 5,
                      "status": "Completed",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-20",
                        "time": "03:00 PM"
                      },
                      "isVirtual": true
                    },
                    {
                      "appointmentId": 6,
                      "status": "Scheduled",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-21",
                        "time": "04:00 PM"
                      },
                      "isVirtual": false
                    },
                    {
                      "appointmentId": 7,
                      "status": "Scheduled",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-22",
                        "time": "10:00 AM"
                      },
                      "isVirtual": true
                    },
                    {
                      "appointmentId": 8,
                      "status": "Completed",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-23",
                        "time": "12:00 PM"
                      },
                      "isVirtual": false
                    },
                    {
                      "appointmentId": 9,
                      "status": "Scheduled",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-24",
                        "time": "11:00 AM"
                      },
                      "isVirtual": true
                    },
                    {
                      "appointmentId": 10,
                      "status": "Completed",
                      "patientId": 1,
                      "schedule": {
                        "date": "2024-11-25",
                        "time": "01:00 PM"
                      },
                      "isVirtual": false
                    }
                  ]
                }
                """;
        try {
            return objectMapper.readValue(response, AppointmentsResponse.class).getAppointments();
        } catch (IOException e) {
            logger.warning("Error parsing response get appointments");
            return Collections.emptyList();
        }
    }

    @Override
    public List<Schedule> getSchedules(int doctorId) {
        return Collections.emptyList();
    }

}
