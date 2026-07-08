import java.util.ArrayList;
import java.util.Scanner;

public class StudentManagementSystem {

    // Holds all student records in memory
    private static ArrayList<Student> students = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        int choice;

        do {
            printMenu();
            choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1:
                    addStudent();
                    break;
                case 2:
                    searchStudent();
                    break;
                case 3:
                    updateStudent();
                    break;
                case 4:
                    deleteStudent();
                    break;
                case 5:
                    displayStudents();
                    break;
                case 6:
                    System.out.println("Exiting... Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please select a number between 1 and 6.");
            }

            System.out.println();
        } while (choice != 6);

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("===== Student Management System =====");
        System.out.println("1. Add Student");
        System.out.println("2. Search Student");
        System.out.println("3. Update Student");
        System.out.println("4. Delete Student");
        System.out.println("5. Display Students");
        System.out.println("6. Exit");
    }

    // ---------- Core operations ----------

    private static void addStudent() {
        System.out.println("--- Add Student ---");

        int id = readInt("Enter ID: ");

        // Prevent duplicate IDs
        if (findStudentById(id) != null) {
            System.out.println("A student with ID " + id + " already exists.");
            return;
        }

        String name = readString("Enter Name: ");
        int age = readInt("Enter Age: ");
        String course = readString("Enter Course: ");

        students.add(new Student(id, name, age, course));
        System.out.println("Student added successfully.");
    }

    private static void searchStudent() {
        System.out.println("--- Search Student ---");
        int id = readInt("Enter ID to search: ");

        Student found = findStudentById(id);
        if (found != null) {
            System.out.println("Student found:");
            System.out.println(found);
        } else {
            System.out.println("No student found with ID " + id);
        }
    }

    private static void updateStudent() {
        System.out.println("--- Update Student ---");
        int id = readInt("Enter ID of student to update: ");

        Student student = findStudentById(id);
        if (student == null) {
            System.out.println("No student found with ID " + id);
            return;
        }

        System.out.println("Current details: " + student);
        System.out.println("Leave a field blank to keep its current value.");

        String name = readString("Enter new Name (" + student.getName() + "): ");
        if (!name.isEmpty()) {
            student.setName(name);
        }

        String ageInput = readString("Enter new Age (" + student.getAge() + "): ");
        if (!ageInput.isEmpty()) {
            try {
                student.setAge(Integer.parseInt(ageInput));
            } catch (NumberFormatException e) {
                System.out.println("Invalid age entered. Age not updated.");
            }
        }

        String course = readString("Enter new Course (" + student.getCourse() + "): ");
        if (!course.isEmpty()) {
            student.setCourse(course);
        }

        System.out.println("Student updated successfully.");
    }

    private static void deleteStudent() {
        System.out.println("--- Delete Student ---");
        int id = readInt("Enter ID of student to delete: ");

        Student student = findStudentById(id);
        if (student == null) {
            System.out.println("No student found with ID " + id);
            return;
        }

        students.remove(student);
        System.out.println("Student deleted successfully.");
    }

    private static void displayStudents() {
        System.out.println("--- All Students ---");

        if (students.isEmpty()) {
            System.out.println("No student records found.");
            return;
        }

        for (Student s : students) {
            System.out.println(s);
        }
    }

    // ---------- Helper methods ----------

    private static Student findStudentById(int id) {
        for (Student s : students) {
            if (s.getId() == id) {
                return s;
            }
        }
        return null;
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}

// Student is kept in the same file as a non-public class.
// Java allows this as long as only one class in the file is public,
// and the public class's name matches the file name.
class Student {
    private int id;
    private String name;
    private int age;
    private String course;

    public Student(int id, String name, int age, String course) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.course = course;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getCourse() {
        return course;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return "ID: " + id +
                " | Name: " + name +
                " | Age: " + age +
                " | Course: " + course;
    }
}