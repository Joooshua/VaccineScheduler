CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    availableTime date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (availableTime, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Appointments (
    AppointmentID INT,
    VaccineName varchar(255) REFERENCES Vaccines(Name),
    appointmentTime date,
    PatientUsername varchar(255) REFERENCES Patients(Username),
    CaregiverUsername varchar(255) REFERENCES Caregivers(Username),
    PRIMARY KEY (AppointmentID)
);