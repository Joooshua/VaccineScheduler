package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Appointment;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import javax.xml.stream.events.Comment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the command name)
        if(tokens.length != 3){
            System.out.println("Error: Incorrect number of parameters, Please type 'create_patient <username> <password>' again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if(usernameExistPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try{
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            currentPatient.saveToDB();
            System.out.println(" *** Patient account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        } finally {
            currentPatient = null;
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Error: Incorrect number of parameters, Please type 'create_caregiver <username> <password>' again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Caregiver account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        } finally {
            currentCaregiver = null;
        }
    }

    private static boolean usernameExistPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // return false if the cursor is not before the first record or if there are no rows in the ResultsSet
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentPatient != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the command name)
        if(tokens.length != 3) {
            System.out.println("Error: Incorrect number of parameters, Please type 'login_patient <username> <password>' again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try{
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null){
            System.out.println("Incorrect Username or password: Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Error: Incorrect number of parameters, Please type 'login_caregiver <username> <password>'!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Incorrect Username or password: Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // check 1: check if the caregiver/patient has already logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Need to log in!");
            return;
        }
        // check 2: the length of the token needs to be exactly 2
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        Date d = null;
        try {
            d = Date.valueOf(tokens[1]);
        } catch (IllegalArgumentException e) {
            System.out.println("Error happened while entering the date");
        }
        // check 3: a valid date should be entered
        if (d == null) {
            System.out.println("Please enter a valid date, the format should be yyyy-mm-dd");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String searchAvailability = "SELECT Username FROM Availabilities WHERE availableTime = ?";
        String searchVaccine = "SELECT Name, Doses FROM Vaccines";
        try {
            PreparedStatement statementAvailability = con.prepareStatement(searchAvailability);
            PreparedStatement statementVaccine = con.prepareStatement(searchVaccine);
            statementAvailability.setDate(1, d);
            ResultSet resultSetAvailability = statementAvailability.executeQuery();
            ResultSet resultSetVaccine = statementVaccine.executeQuery();
            // check 4: check if there is an available time slot at current date
            if (!resultSetAvailability.isBeforeFirst()) {
                System.out.println("No more available caregivers at current date");
                return;
            }
            System.out.println("Available Caregivers at " + tokens[1] + ":");
            while (resultSetAvailability.next()) {
                String username = resultSetAvailability.getString("Username");
                System.out.print(" | " + username);
            }
            System.out.println(" | \n\nAvailable doses left of each vaccine:");
            while (resultSetVaccine.next()) {
                String vaccineName = resultSetVaccine.getString("Name");
                int availableDoses = resultSetVaccine.getInt("Doses");
                System.out.println("Vaccine Name: " + vaccineName + " Available Doses: " + availableDoses);
            }
            System.out.println("\nYou can type 'reserve <date> <vaccine>' to make reservation.");
        } catch (SQLException e) {
            System.out.println("Please enter a valid date");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // check 1: check if the current logged-in
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the command name)
        if (tokens.length != 3) {
            System.out.println("Error: Incorrect number of arguments. Please type 'reserve <date> <vaccine>'!");
            return;
        }

        String date = tokens[1];
        String vaccineName = tokens[2];
        Date d = null;
        Vaccine vaccine = null;
        String caregiverUsername = null;
        // try to get the date
        try {
            d = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Error happened while entering the date");
        }
        // check 3: a valid date should be entered
        if (d == null) {
            System.out.println("Please enter a valid date, the format should be yyyy-mm-dd");
            return;
        }
        // check 4: check if this patient has already scheduled an appointment at current date
        // if yes, no more appointment should be made
        if (checkAppointment(d, currentPatient.getUsername(), false)) {
            System.out.println("You have already have an appointment at current date, please enter another date");
            return;
        }
        // try to get the vaccine
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking doses.");
            e.printStackTrace();
        }
        // check 4: check if the doses are enough
        if (vaccine == null) {
            System.out.println("Not enough doses available, please add doses first.");
            return;
        }
        // start the connection
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // randomly assign one of the available caregiver on this day to the current patient
        String selectCaregivers = "SELECT * FROM Availabilities WHERE availableTime = ? ORDER BY NEWID()";
        try {
            PreparedStatement statementCaregivers = con.prepareStatement(selectCaregivers);
            statementCaregivers.setDate(1, d);
            ResultSet resultSet = statementCaregivers.executeQuery();
            // obtain the username of the caregiver
            while (resultSet.next())
                caregiverUsername = resultSet.getString("Username");
        } catch (SQLException e) {
            System.out.println("Error happened while assigning the caregiver");
        }

        // check 5: check if the correct caregiver is assigned
        if (caregiverUsername == null) {
            System.out.println("Error: no caregiver is assigned. No more caregiver is available");
            return;
        }
        Appointment appointment = null;
        try {
            // a new entry of the Appointment tables need to be added
            appointment = new Appointment.AppointmentBuilder(vaccineName, Date.valueOf(date), currentPatient.getUsername(), caregiverUsername).build();
            appointment.saveToDB();
            // if there is still vaccine, decrease the vaccine number by 1
            vaccine.decreaseAvailableDoses(1);
            // current day of the caregiver needs to be removed from the accountability table
            removeAvailability(Date.valueOf(date), caregiverUsername);
            System.out.println("Reserve successfully with " + caregiverUsername + "!");
        } catch (SQLException e) {
            System.out.println("Error happened while creating a new appointment");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    // helper function for "reserve" function
    public static void removeAvailability(Date d, String name) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String removeAvailability = "DELETE FROM Availabilities WHERE availableTime = ? AND Username = ?";
        try{
            PreparedStatement statement = con.prepareStatement(removeAvailability);
            statement.setDate(1, d);
            statement.setString(2, name);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the command name)
        if (tokens.length != 2) {
            System.out.println("Error: Incorrect number of parameters. Please type 'upload_availability <date>'");
            return;
        }
        Date d = null;
        try {
            d = Date.valueOf(tokens[1]);
        } catch (IllegalArgumentException e) {
            System.out.println("Error happened while entering the date:");
        }
        if (d == null) {
            System.out.println("Please enter a valid date");
            return;
        }

        String caregiverUsername = currentCaregiver.getUsername();
        System.out.println("Searching for the appointment to check whether you have an appointment at current date ...");
        // check if current date has already been scheduled an appointment
        if (!checkAppointment(d, caregiverUsername, true)) {
            System.out.println("You did not schedule an appointment at this date");
            try {
                currentCaregiver.uploadAvailability(d);
                System.out.println("Availability uploaded!");
            } catch (SQLException e) {
                System.out.println("Error occurred when uploading availability. Do not enter the repeated date");
            }
        } else {
            System.out.println("You've already have an appointment at current date, do not enter this date");
        }
    }

    public static boolean checkAppointment(Date d, String username, boolean isCaregiver) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String searchAppointment;
        if (isCaregiver) {
            searchAppointment = "SELECT * FROM Appointments WHERE appointmentTime = ? AND CaregiverUsername = ?";
        } else {
            searchAppointment = "SELECT * FROM Appointments WHERE appointmentTime = ? AND PatientUsername = ?";
        }
        try {
            PreparedStatement statement = con.prepareStatement(searchAppointment);
            statement.setDate(1, d);
            statement.setString(2, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error happened while searching for the appointment");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        // Check 1: check if the patient/caregiver has logged in
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please log in as the patient or caregiver");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the command name)
        if (tokens.length != 2) {
            System.out.println("Error: Incorrect number of arguments. Please try again!");
            return;
        }
        int ID = -1;
        String appointmentID = tokens[1];
        // try to get the int value of the input number
        try {
            ID = Integer.valueOf(appointmentID);
            System.out.println("You want to cancel the appointment: " + ID);
        } catch (IllegalArgumentException e) {
            System.out.println("Error happened while entering the Appointment ID");
        }
        // check 3: the input number should be valid
        if (ID == -1) {
            System.out.println("Please enter a valid appointment ID");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAppointment = "SELECT VaccineName, appointmentTime, CaregiverUsername FROM Appointments WHERE AppointmentID = ?";
        String cancelAppointment = "DELETE FROM Appointments WHERE appointmentID = ?";
        String reuploadAvailability = "INSERT INTO Availabilities VALUES (?, ?)";
        String readdDoses = "UPDATE vaccines SET Doses = ? WHERE name = ?";
        try{
            // get the vaccine name, appointment time, caregiver name before cancelling
            PreparedStatement statementGet = con.prepareStatement(getAppointment);
            statementGet.setInt(1, ID);
            ResultSet resultSet = statementGet.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("Error: There is no appointment using this appointment ID");
            } else {
                while (resultSet.next()) {
                    String vaccineName = resultSet.getString("VaccineName");
                    Date appointmentTime = resultSet.getDate("appointmentTime");
                    String caregiverName = resultSet.getString("CaregiverUsername");
                    // cancel the appointment
                    PreparedStatement statementCancel = con.prepareStatement(cancelAppointment);
                    statementCancel.setInt(1, ID);
                    statementCancel.executeUpdate();
                    // reupload the availability of the current caregiver at the current date
                    PreparedStatement statementReupload = con.prepareStatement(reuploadAvailability);
                    statementReupload.setDate(1, appointmentTime);
                    statementReupload.setString(2, caregiverName);
                    statementReupload.executeUpdate();
                    // readd the doses being assigned
                    Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
                    PreparedStatement statementReadd = con.prepareStatement(readdDoses);
                    statementReadd.setInt(1, vaccine.getAvailableDoses()+1);
                    statementReadd.setString(2, vaccineName);
                    statementReadd.executeUpdate();
                }
                System.out.println("Successfully cancel!");
            }
        } catch (SQLException e) {
            System.out.println("Error happened while delete the current appointment");
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens){
        // TODO: Part 2
        // Check 1: check if the patient/caregiver has logged in
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please log in as the patient or caregiver");
            return;
        }
        // check 2: the length for tokens need to be exactly 1 to include all information (with the command name)
        if (tokens.length != 1) {
            System.out.println("Error: Incorrect number of arguments. Please try again!");
            return;
        }

        Queue<Appointment> appointments = null;
        Appointment appointment = null;
        try {
            if (currentPatient != null) appointments = new Appointment.AppointmentGetter(currentPatient.getUsername(), false).get();
            if (currentCaregiver != null) appointments = new Appointment.AppointmentGetter(currentCaregiver.getUsername(), true).get();
            if (appointments.isEmpty()) System.out.println("No appointments scheduled");
            while (!appointments.isEmpty()) {
                appointment = appointments.poll();
                System.out.print(" | appointmentID: " + appointment.getAppointmentID());
                System.out.print(" | vaccine name: " + appointment.getVaccineName());
                System.out.print(" | date: " + appointment.getAppointmentTime());
                if(currentPatient != null) System.out.print(" | caregiver name: " + appointment.getCaregiverUsername());
                if(currentCaregiver != null) System.out.print(" | patient name: " + appointment.getPatientUsername());
                System.out.println();
            }
        } catch (SQLException e) {
            System.out.println("Error happened while searching appointment");
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        // check 1: hasn't logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Error: no user has logged in, logout invalid");
            return;
        }
        if (currentCaregiver != null) {
            String username = currentCaregiver.getUsername();
            System.out.println("Caregiver logged out: " + username);
            currentCaregiver = null;
        }
        if (currentPatient != null) {
            String username = currentPatient.getUsername();
            System.out.println("Patient logged out: " + username);
            currentPatient = null;
        }
    }
}
