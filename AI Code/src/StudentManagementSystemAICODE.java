import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Entry point and console UI for the Student Management System.
 * <p>
 * Responsibilities are deliberately separated:
 * - {@link Student}                 -> data + field-level validation
 * - {@link StudentRepository}       -> storage contract (CRUD)
 * - {@link InMemoryStudentRepository} -> in-memory implementation backed by a Map
 * - {@link StudentManagementSystemAICODE} -> menu, input handling, orchestration
 * <p>
 * This keeps business rules (what a valid student looks like, how storage works)
 * independent of how the user interacts with the program, so either side can be
 * swapped or extended (e.g. a database-backed repository, or a GUI front end)
 * without touching the other.
 */
public class StudentManagementSystemAICODE {

    private static final Logger LOGGER = Logger.getLogger(StudentManagementSystemAICODE.class.getName());
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final StudentRepository repository = new InMemoryStudentRepository();

    public static void main(String[] args) {
        runApplication();
    }

    /** Main program loop: show menu, read a choice, dispatch, repeat until Exit. */
    private static void runApplication() {
        boolean running = true;

        while (running) {
            printMenu();
            MenuOption option = readMenuOption();

            try {
                switch (option) {
                    case ADD -> addStudent();
                    case SEARCH -> searchStudent();
                    case UPDATE -> updateStudent();
                    case DELETE -> deleteStudent();
                    case DISPLAY -> displayStudents();
                    case EXIT -> running = false;
                }
            } catch (StudentNotFoundException | DuplicateStudentException | InvalidInputException e) {
                // Expected, user-facing failures: show a clean message, keep the app running.
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                // Anything unanticipated: log the full detail for diagnosis, but never crash the UI.
                LOGGER.log(Level.SEVERE, "Unexpected error occurred", e);
                System.out.println("An unexpected error occurred. Please try again.");
            }

            System.out.println();
        }

        System.out.println("Goodbye!");
        SCANNER.close();
    }

    // ---------- Menu ----------

    private static void printMenu() {
        System.out.println("===== Student Management System =====");
        for (MenuOption option : MenuOption.values()) {
            System.out.println(option.getCode() + ". " + option.getLabel());
        }
    }

    // ---------- Feature operations ----------

    private static void addStudent() throws InvalidInputException {
        System.out.println("--- Add Student ---");

        int id = readPositiveInt("Enter ID: ");
        if (repository.existsById(id)) {
            System.out.println("A student with ID " + id + " already exists.");
            return;
        }

        String name = readNonEmptyString("Enter Name: ");
        int age = readIntInRange("Enter Age: ", 1, 120);
        String course = readNonEmptyString("Enter Course: ");

        try {
            Student student = new Student(id, name, age, course);
            repository.add(student);
            System.out.println("Student added successfully.");
        } catch (IllegalArgumentException e) {
            // Student's own validation caught something the input helpers didn't.
            throw new InvalidInputException(e.getMessage());
        }
    }

    private static void searchStudent() throws InvalidInputException {
        System.out.println("--- Search Student ---");
        System.out.println("1. Search by ID");
        System.out.println("2. Search by Name");
        int choice = readIntInRange("Choose an option: ", 1, 2);

        if (choice == 1) {
            int id = readPositiveInt("Enter ID: ");
            repository.findById(id).ifPresentOrElse(
                    s -> System.out.println("Student found:\n" + s),
                    () -> System.out.println("No student found with ID " + id)
            );
        } else {
            String keyword = readNonEmptyString("Enter name keyword: ");
            List<Student> matches = repository.findByNameContains(keyword);

            if (matches.isEmpty()) {
                System.out.println("No students matched \"" + keyword + "\".");
            } else {
                printTableHeader();
                matches.forEach(System.out::println);
            }
        }
    }

    private static void updateStudent() throws InvalidInputException {
        System.out.println("--- Update Student ---");
        int id = readPositiveInt("Enter ID of student to update: ");

        Student existing = repository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("No student found with ID " + id));

        System.out.println("Current details:\n" + existing);
        System.out.println("Leave a field blank to keep its current value.");

        String name = readOptionalString("New Name (" + existing.getName() + "): ");
        String ageInput = readOptionalString("New Age (" + existing.getAge() + "): ");
        String course = readOptionalString("New Course (" + existing.getCourse() + "): ");

        try {
            repository.update(id, student -> {
                if (!name.isEmpty()) {
                    student.setName(name);
                }
                if (!ageInput.isEmpty()) {
                    student.setAge(Integer.parseInt(ageInput));
                }
                if (!course.isEmpty()) {
                    student.setCourse(course);
                }
            });
            System.out.println("Student updated successfully.");
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Age must be a whole number.");
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(e.getMessage());
        }
    }

    private static void deleteStudent() {
        System.out.println("--- Delete Student ---");
        int id = readPositiveInt("Enter ID of student to delete: ");
        repository.delete(id); // throws StudentNotFoundException, handled by the caller's catch block
        System.out.println("Student deleted successfully.");
    }

    private static void displayStudents() {
        System.out.println("--- All Students ---");
        List<Student> all = repository.findAll();

        if (all.isEmpty()) {
            System.out.println("No student records found.");
            return;
        }

        System.out.println("Sort by: 1. ID   2. Name   3. Age   (press Enter for ID)");
        String sortChoice = readOptionalString("Choice: ");

        Comparator<Student> comparator = switch (sortChoice) {
            case "2" -> Comparator.comparing(Student::getName, String.CASE_INSENSITIVE_ORDER);
            case "3" -> Comparator.comparingInt(Student::getAge);
            default -> Comparator.comparingInt(Student::getId);
        };

        printTableHeader();
        all.stream().sorted(comparator).forEach(System.out::println);
    }

    private static void printTableHeader() {
        System.out.printf("%-6s %-20s %-5s %-15s%n", "ID", "Name", "Age", "Course");
        System.out.println("-".repeat(50));
    }

    // ---------- Input helpers (all retry-on-invalid, never throw for bad input) ----------

    private static int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value <= 0) {
                    System.out.println("Please enter a positive number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    System.out.println("Please enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    /** Like readNonEmptyString, but an empty answer is valid (used by "leave blank to skip" prompts). */
    private static String readOptionalString(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    private static MenuOption readMenuOption() {
        while (true) {
            System.out.print("Enter your choice: ");
            String input = SCANNER.nextLine().trim();
            try {
                int code = Integer.parseInt(input);
                return MenuOption.fromCode(code);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            } catch (InvalidInputException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

// =====================================================================================
// Domain model
// =====================================================================================

/**
 * Represents a single student record.
 * Validation lives here (not in the UI layer) so a Student object is never in an
 * invalid state, regardless of who constructs or edits it.
 */
class Student implements Comparable<Student> {
    private int id;
    private String name;
    private int age;
    private String course;

    public Student(int id, String name, int age, String course) {
        setId(id);
        setName(name);
        setAge(age);
        setCourse(course);
    }

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

    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be a positive integer.");
        }
        this.id = id;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    public void setAge(int age) {
        if (age < 1 || age > 120) {
            throw new IllegalArgumentException("Age must be between 1 and 120.");
        }
        this.age = age;
    }

    public void setCourse(String course) {
        if (course == null || course.isBlank()) {
            throw new IllegalArgumentException("Course cannot be empty.");
        }
        this.course = course.trim();
    }

    @Override
    public int compareTo(Student other) {
        return Integer.compare(this.id, other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Student)) {
            return false;
        }
        Student other = (Student) o;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%-6d %-20s %-5d %-15s", id, name, age, course);
    }
}

/** The six actions the console menu supports, each tied to its displayed number and label. */
enum MenuOption {
    ADD(1, "Add Student"),
    SEARCH(2, "Search Student"),
    UPDATE(3, "Update Student"),
    DELETE(4, "Delete Student"),
    DISPLAY(5, "Display Students"),
    EXIT(6, "Exit");

    private final int code;
    private final String label;

    MenuOption(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static MenuOption fromCode(int code) throws InvalidInputException {
        for (MenuOption option : values()) {
            if (option.code == code) {
                return option;
            }
        }
        throw new InvalidInputException("Invalid menu choice: " + code + ". Pick a number from 1 to 6.");
    }
}

// =====================================================================================
// Persistence layer (in-memory today, swappable for a database later)
// =====================================================================================

/** Storage contract for students, independent of how or where records are kept. */
interface StudentRepository {
    void add(Student student) throws DuplicateStudentException;

    Optional<Student> findById(int id);

    List<Student> findByNameContains(String keyword);

    void update(int id, Consumer<Student> updater) throws StudentNotFoundException;

    void delete(int id) throws StudentNotFoundException;

    List<Student> findAll();

    boolean existsById(int id);
}

/** Keeps students in a Map keyed by ID for O(1) lookup, update, and delete. */
class InMemoryStudentRepository implements StudentRepository {
    // LinkedHashMap preserves insertion order, so "Display Students" shows a stable order by default.
    private final Map<Integer, Student> studentsById = new LinkedHashMap<>();

    @Override
    public void add(Student student) throws DuplicateStudentException {
        if (studentsById.containsKey(student.getId())) {
            throw new DuplicateStudentException("Student with ID " + student.getId() + " already exists.");
        }
        studentsById.put(student.getId(), student);
    }

    @Override
    public Optional<Student> findById(int id) {
        return Optional.ofNullable(studentsById.get(id));
    }

    @Override
    public List<Student> findByNameContains(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return studentsById.values().stream()
                .filter(s -> s.getName().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }

    @Override
    public void update(int id, Consumer<Student> updater) throws StudentNotFoundException {
        Student student = studentsById.get(id);
        if (student == null) {
            throw new StudentNotFoundException("No student found with ID " + id);
        }
        updater.accept(student);
    }

    @Override
    public void delete(int id) throws StudentNotFoundException {
        if (studentsById.remove(id) == null) {
            throw new StudentNotFoundException("No student found with ID " + id);
        }
    }

    @Override
    public List<Student> findAll() {
        return new ArrayList<>(studentsById.values());
    }

    @Override
    public boolean existsById(int id) {
        return studentsById.containsKey(id);
    }
}

// =====================================================================================
// Custom exceptions -- each names exactly one failure mode, so callers can react precisely
// =====================================================================================

/** Thrown when an operation targets a student ID that isn't in the repository. */
class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(String message) {
        super(message);
    }
}

/** Thrown when trying to add a student whose ID already exists. */
class DuplicateStudentException extends RuntimeException {
    public DuplicateStudentException(String message) {
        super(message);
    }
}

/** Thrown for user-supplied data that fails validation (checked, so callers must handle it deliberately). */
class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}