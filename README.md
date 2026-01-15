# MyShiftApp

MyShiftApp is an Android application designed to simplify shift scheduling and workforce management for both employees and managers.

The goal of the project is to help managers build fair and efficient shift schedules based on employee availability, while providing employees with a clear and simple interface to manage their work preferences.

---

## 🚀 Current Features

### Authentication
- Login using **email and password**
- First-time login requires **password change**
- Role-based navigation (Manager / Employee)
- Authentication currently implemented using **Firebase Authentication**

### User Roles
- **Employee**
- Personalized welcome screen
- Foundation for availability reporting and shift viewing

- **Manager**
- Personalized welcome screen
- Foundation for shift configuration and management

### Architecture
- Built with **Fragments** (single-activity architecture)
- UI implemented using **XML with LinearLayout**
- Navigation handled via `FragmentManager`
- Temporary local logic used where backend features are still in progress

---

## 🛠️ Technologies Used

- **Java**
- **Android SDK**
- **Firebase Authentication**
- **Firebase Firestore** (connected, logic in progress)
- **Material Design Components**
- **Fragment-based navigation**

---

## 🔮 Planned Features (Next Stages)

- Employee availability selection (green / unavailable)
- Manager-defined shift types (morning / evening / night)
- Automatic shift schedule generation
- Salary slips and monthly summaries
- Integration of **AI-based scheduling assistance**
- Full data persistence using Firebase Firestore

---

## 📌 Project Status

This project is currently under active development.
The current version focuses on authentication, role separation, and application structure.

---

## 👩‍💻 Authors

Developed as part of an academic software engineering project.
