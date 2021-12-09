package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Appointment {
    private final int appointmentID;
    private final String vaccineName;
    private final Date appointmentTime;
    private final String patientUsername;
    private final String caregiverUsername;

    private Appointment(AppointmentBuilder builder) {
        this.appointmentID = builder.appointmentID;
        this.vaccineName = builder.vaccineName;
        this.appointmentTime = builder.appointmentTime;
        this.patientUsername = builder.patientUsername;
        this.caregiverUsername = builder.caregiverUsername;
    }

    private Appointment(AppointmentGetter getter) {
        this.appointmentID = getter.appointmentID;
        this.vaccineName = getter.vaccineName;
        this.appointmentTime = getter.appointmentTime;
        this.patientUsername = getter.patientUsername;
        this.caregiverUsername = getter.caregiverUsername;
    }

    // Getters
    public int getAppointmentID() { return appointmentID; }

    public String getVaccineName() {return vaccineName; }

    public Date getAppointmentTime() { return appointmentTime; }

    public String getPatientUsername() { return patientUsername; }

    public String getCaregiverUsername() { return caregiverUsername; }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addCaregiver = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addCaregiver);
            statement.setInt(1, this.appointmentID);
            statement.setString(2, this.vaccineName);
            statement.setDate(3, this.appointmentTime);
            statement.setString(4, this.patientUsername);
            statement.setString(5, this.caregiverUsername);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AppointmentBuilder {
        private int appointmentID;
        private String vaccineName;
        private Date appointmentTime;
        private String patientUsername;
        private String caregiverUsername;

        public AppointmentBuilder (String vaccineName, Date appointmentTime, String patientUsername, String caregiverUsername) {
            try {
                this.appointmentID = getIdentifier();
            } catch (SQLException e) {
                System.out.println("Error happened while search for the identifiers");
                e.printStackTrace();
            }
            this.vaccineName = vaccineName;
            this.appointmentTime = appointmentTime;
            this.patientUsername = patientUsername;
            this.caregiverUsername = caregiverUsername;
        }

        public Appointment build() throws SQLException {
            return new Appointment(this);
        }
    }

    public static int getIdentifier() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String checkID = "SELECT * FROM Appointments WHERE AppointmentID = ?";
        try {
            PreparedStatement statement = con.prepareStatement(checkID);
            int randomNum = new Random().nextInt(90000001) + 10000000;
            statement.setInt(1, randomNum);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.getRow() != 0) {
                System.out.println("generating a new appointment ID" + randomNum);
                randomNum = new Random().nextInt(90000001) + 10000000;
                statement.setInt(1, randomNum);
                resultSet = statement.executeQuery();
            }
            return randomNum;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AppointmentGetter {
        private int appointmentID;
        private String vaccineName;
        private Date appointmentTime;
        private String patientUsername;
        private String caregiverUsername;
        private boolean flag; // check if the username passed in is caregiver(true) or patient(false)

        public AppointmentGetter(String username, boolean flag) {
            this.flag = flag;
            if (this.flag) {
                this.caregiverUsername = username;
            } else {
                this.patientUsername = username;
            }
        }

        public Queue<Appointment> get() throws SQLException {
            Queue<Appointment> appointmentsQueue = new LinkedList<>();
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAppointment;
            if (this.flag) {
                getAppointment = "SELECT AppointmentID, VaccineName, appointmentTime, PatientUsername FROM Appointments WHERE CaregiverUsername = ?";
            } else {
                getAppointment = "SELECT AppointmentID, VaccineName, appointmentTime, CaregiverUsername FROM Appointments WHERE PatientUsername = ?";
            }
            try {
                PreparedStatement statement = con.prepareStatement(getAppointment);
                if (this.flag) {
                    statement.setString(1, this.caregiverUsername);
                } else {
                    statement.setString(1, this.patientUsername);
                }
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    int appointmentID = resultSet.getInt("AppointmentID");
                    String vaccineName = resultSet.getString("VaccineName");
                    Date appointmentTime = resultSet.getDate("appointmentTime");
                    if (this.flag) {
                        String patientUsername = resultSet.getString("PatientUsername");
                        this.patientUsername = patientUsername;
                    } else {
                        String caregiverUsername = resultSet.getString("CaregiverUsername");
                        this.caregiverUsername = caregiverUsername;
                    }
                    this.appointmentID = appointmentID;
                    this.vaccineName = vaccineName;
                    this.appointmentTime = appointmentTime;
                    appointmentsQueue.add(new Appointment(this));
                }
                return appointmentsQueue;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
