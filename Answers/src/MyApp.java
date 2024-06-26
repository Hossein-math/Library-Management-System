// main class for handling the CLI

import javax.naming.NoPermissionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

public class  MyApp {
    // database information
    public static final String DB_URL = "jdbc:mysql://localhost/library";
    public static final String DB_USERNAME = "LMSjava";
    public static final String DB_PASSWORD = "lmsjava1234";

    // library information
    public static final Library library = new Library(); // our library
    public static final Path PATH = Paths.get(".library.txt");

    // scanner for input from client
    public static Scanner input = new Scanner(System.in);

    public static void main(String[] args) {
        // check to library is existed or not
        if (!Files.exists(PATH)) {
            System.out.println("Welcome to LMS(Library Management System)!");
            System.out.println("You should create your own library first\n");
            while (!createLibrary()); // first we should create a library to use it

            System.out.println("Now you should add a new admin to your library!");
            // add a new admin user
            while (true) {
                System.out.println("\nEnter these information about your admin:");
                try {
                    // get admin name
                    System.out.print("Name: ");
                    String name = input.nextLine();

                    // get admin phone number
                    System.out.print("Phone number: ");
                    String phoneNumber = input.nextLine();

                    // get admin password
                    System.out.print("Password: ");
                    String password = input.nextLine();

                    Admin admin = new Admin(name, phoneNumber, password);

                    if (library.addUser(admin, password) == 0) {
                        // delete library file
                        File libraryFile = PATH.toFile();
                        libraryFile.delete();

                        System.out.println("Terminating...");
                        System.exit(1);
                    }

                    library.setUser(admin); // this is log in now
                    System.out.printf("You are now logged in! Your ID is %s.%n",
                            library.getUser().getUniqueID());
                } catch (IllegalArgumentException e) {
                    System.err.printf("%s%n", e.getMessage());
                    System.out.println("Try again.");
                    continue;
                }

                break;
            }
        }
        else {
            try (Scanner retrieveLibrary = new Scanner(PATH)) {
                library.setName(retrieveLibrary.nextLine());
                library.setCapacity(retrieveLibrary.nextInt());
                library.setOperatingHours(retrieveLibrary.next());

            } catch (IOException e) {
                System.err.printf("%s%n", e.getMessage());
                e.printStackTrace();
            }
        }
        // welcome to library
        System.out.printf("Welcome to %s library.%n", library.getName());
        System.out.println("For more information enter command <lib man>");
        System.out.println("Enter your command:");

        while (true) {
            String command = null;

            try {
                if (library.getUser() == null)
                    System.out.print(">>> ");
                else if (library.getUser() instanceof Admin)
                    System.out.print("Admin> ");
                else
                    System.out.printf("%s> ", library.getUser().getName());
                command = input.nextLine();
                CLI(command.trim());
            } catch (InvalidParameterException e) {
                System.err.println("Invalid username. please try again.");
            } catch (IllegalArgumentException e) {
                if (e.getMessage() == null)
                    System.out.printf("command <%s> %s%n", command.trim(), "Not Found.");
                else
                    System.out.printf("%s%n", e.getMessage());
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.printf("command <%s> %s%n", command.trim(), "Not Found.");
            } catch (NoPermissionException e) {
                System.out.printf("%s%n", e.getMessage());
            } catch (InputMismatchException e) {
                System.out.printf("Invalid input. proccess cancled...");
            }
        }
    }

    private static boolean createLibrary() {
        System.out.println("Please enter these information about your library:");

        try {
            System.out.print("name : ");
            library.setName(input.nextLine());

            System.out.print("capacity : ");
            library.setCapacity(input.nextInt());
            input.nextLine();

            System.out.print("operating hours (format : xx-xx): ");
            library.setOperatingHours(input.nextLine());

            library.saveLibrary();

            System.out.println("Your library was created successfully.\n");
        } catch (InputMismatchException e) {
            System.err.printf("%s%n", e.getMessage());
            System.err.println("creation was not successful. Terminating program.");
            System.exit(1); // creation was not successful

        } catch (IllegalArgumentException e) {
            System.err.printf("%s%n%n", e.getMessage());
            System.out.println("Try again.");
            return false;
        } catch (SecurityException securityException) {
            System.err.println("Write permission denied. Terminating.");
            System.exit(1); // terminate the program

        } catch (FileNotFoundException fileNotFoundException) {
            System.err.println("Error opening file. Terminating.");
            System.exit(1); // terminate the program
        }
        return true;
    }

    public static void CLI(String command) throws IllegalArgumentException, NoPermissionException,
            InputMismatchException {
        if (command.equals("whoami"))
            if (library.getUser() == null)
                System.out.println("No one one has logged in yet.");
            else
                System.out.println(library.getUser());
        // command lib man
        else if (command.matches("lib\\sman")) {
            System.out.println("You can use these commands :\n");
            if (library.getUser() instanceof Admin)
                Manual.printAdmin();

            Manual.printNormalUser();
        }
        else if (command.matches("lib\\sget\\srented\\sbooks")) {
            if (library.getUser() == null)
                throw new NoPermissionException("You should first login.");
            else if (library.getUser() instanceof Admin)
                throw new NoPermissionException("You are admin!You can't rent a book:)");

            ArrayList<Book> books = ((NormalUser) library.getUser()).getRentBooks();

            if (books.size() == 1)
                System.out.println("Your rented book is :");
            else
                System.out.println("Your rented books are: ");

            int numberOfRows = 1;
            for (Book book : books) {
                System.out.printf("%d.%s", numberOfRows++, book.getTitle());
                System.out.println();
            }
        }
        // command lib get hrs
        else if (command.matches("lib\\sget\\shrs"))
            System.out.print(library.getOperatingHours());
        else if (command.matches("lib\\sget\\savailable\\sbooks"))
            library.getAvailableBooks();
        // command lib remove member
        else if (command.matches("lib\\slogin\\s\\d+")) {
            //command lib login
            if (library.getUser() != null)
                throw new NoPermissionException("Another account is still logged in! " +
                        "Log out and try again.");
            String[] temp = command.split("\\s");
            library.setUser(library.login(temp[2]));
            if (library.getUser() == null)
                throw new InvalidParameterException();
            else if (library.getUser() instanceof NormalUser)
                System.out.printf("Hello %s!You logged in successfully.%n", library.getUser().getName());
            else {
                Admin adminUser = (Admin) library.getUser();
                for (int i = 0; i < 3; i++) {
                    System.out.printf("Enter password for %s: ", adminUser.getName());
                    String password = input.nextLine();
                    if (adminUser.verify(password)) {
                        System.out.printf("Hello %s! You logged in successfully.%n", adminUser.getName());
                        return;
                    } else
                        System.out.println("Invalid password. Try again.");
                }
                library.setUser(null);
                throw new IllegalArgumentException("3 incorrect password attempts...");
            }
        } else if (command.matches("lib\\slogout")) {
            if (library.getUser() == null)
                System.out.println("No one has logged in yet");
            else {
                System.out.printf("%s logged out successfully.%n", library.getUser().getName());
                library.setUser(null);
            }
        } else if (command.matches("lib\\sreturn\\s.+")) {
            // return a book
            String bookName = command.substring(11);
            ArrayList<Book> books = ((NormalUser) library.getUser()).getRentBooks();
            boolean hasBook = false;
            int bookID = 0;
            for (Book book : books)
                if (book.getTitle().equals(bookName)) {
                    hasBook = true;
                    bookID = book.getUniqueID();
                }
            if (hasBook) {
                library.returnBook(bookID);
                System.out.printf("Book <%s> returned to library.%n", bookName);
            } else
                System.out.println("You didn't rent this book:)");
        } else if (command.matches("lib\\srent\\s\"?[^\"]*\"?\\s\"?[^\"]*\"?\\s\\d{7,}")) {
            if (library.getUser() == null)
                throw new NoPermissionException("You should first login.");
            else if (library.getUser() instanceof NormalUser)
                throw new NoPermissionException("You don't have permission to rent a book for " +
                        "another person.");
            else
                for (int i = 0; i < 3; i++) {
                    System.out.printf("Enter password for %s: ", library.getUser().getName());
                    String password = input.nextLine();
                    if (((Admin) library.getUser()).verify(password))
                        break;
                    else if (i == 2) {
                        System.out.println("3 incorrect password attempts...");
                        library.setUser(null);
                        return;
                    } else
                        System.out.println("Invalid password. Try again.");
                }
            command = command.substring(9);

            String[] temp = new String[2];
            for (int i = 0; i < 2; i++) {
                if (command.startsWith("\"")) {
                    command = command.substring(1);
                    temp[i] = command.substring(0, command.indexOf("\""));
                    command = command.replaceFirst(temp[i] + "\" ", "");
                } else {
                    temp[i] = command.substring(0, command.indexOf(" "));
                    command = command.replaceFirst(temp[i] + " ", "");
                }
            }

            String bookName = temp[0], memberName = temp[1];
            int memberID = Integer.parseInt(command);

            // get books with this name
            ArrayList<Book> books = library.searchBook(bookName);

            if (books.size() == 0) {
                System.out.printf("book %s not found or not available!%n", bookName);
                return;
            }

            Rent rent = null;
            NormalUser user = null;

            try {
                user = new NormalUser(memberID, memberName);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }

            if (user.getRentBooks().size() >= 5) {
                System.out.println("You have already rented 5 books and you can no longer " +
                        "rent a book");
                return;
            } if (books.size() == 1)
                rent = new Rent(books.get(0), user);
            else {
                System.out.println("Which book?");

                // show selections
                int numberOfRows = 1;
                for (Book book : books) {
                    System.out.printf("%d. ID = %d, Author = %d", numberOfRows,
                            book.getUniqueID(), book.getAuthor());
                    numberOfRows++;
                }

                // select choice
                int choice = 1;
                for (int i = 0; i < 3; i++) {
                    if (library.getUser() == null)
                        System.out.print(">>> ");
                    else if (library.getUser() instanceof Admin)
                        System.out.print("Admin> ");
                    else
                        System.out.printf("%s> ", library.getUser().getName());
                    choice = input.nextInt();
                    if (choice > numberOfRows) {
                        if (i == 2) {
                            System.out.println("3 invalid choice...");
                            return;
                        }
                        System.out.print("Invalid choice. please try again.");
                    } else
                        break;
                }

                // create rent class
                rent = new Rent(books.get(choice - 1), user);

            }
            int rentalID = user.rentBook(rent);
            if (rentalID == 0)
                return;
            System.out.printf("Book %s was rented for %s successfully.", bookName, user.getName());
            System.out.printf("rental ID = %d%n", rentalID);
        } else if (command.matches("lib\\sadd\\smember\\s\"?[^\"]*\"?\\s\\+?[\\d-]+\\s?[^(\"\\s)]*")) {
            // commadn lib add member
            // if he didn't admin user yet throw exception
            if (library.getUser() instanceof Admin) {
                for (int i = 0; i < 3; i++) {
                    System.out.printf("Enter password for %s: ", library.getUser().getName());
                    String password = input.nextLine();
                    if (((Admin) library.getUser()).verify(password))
                        break;
                    else if (i == 2) {
                        System.out.println("3 incorrect password attempts...");
                        library.setUser(null);
                        return;
                    } else
                        System.out.println("Invalid password. Try again.");
                }

                String name, phoneNumber;
                int userID;

                String[] temp1 = command.split("\"");
                if (temp1.length == 3) {
                    name = temp1[1].trim();

                    String[] temp2 = temp1[2].trim().split("\\s");
                    phoneNumber = temp2[0].trim();
                    User user;
                    if (temp2.length == 2) {
                        user = new Admin(name, phoneNumber, temp2[1].trim());
                        userID = library.addUser(user, temp2[1].trim());
                    } else {
                        user = new NormalUser(name, phoneNumber);
                        userID = library.addUser((NormalUser) user);
                    }
                } else {
                    String[] temp = command.split("\\s");

                    if (temp.length > 6)
                        throw new IllegalArgumentException(
                                "You should use \" character to split string with space");

                    name = temp[3].trim();
                    phoneNumber = temp[4].trim();

                    User user;
                    if (temp.length == 6) {
                        user = new Admin(name, phoneNumber, temp[5].trim());
                        userID = library.addUser(user, temp[5].trim());
                    } else {
                        user = new NormalUser(name, phoneNumber);
                        userID = library.addUser((NormalUser) user);
                    }
                }

                // prompt the user
                System.out.printf("User %s with ID = %d added to library.%n", name, userID);
            } // end if (library.user instanceof Admin)
            else
                throw new NoPermissionException("You don't have permission to add members!");
        } else if (command.matches("lib\\sremove\\smember\\s\\d{7,}")) {
            if (library.getUser() == null)
                throw new NoPermissionException("You should first login.");
            else if (library.getUser() instanceof NormalUser)
                throw new NoPermissionException("You don't have permission to remove members.");
            else
                for (int i = 0; i < 3; i++) {
                    System.out.printf("Enter password for %s: ", library.getUser().getName());
                    String password = input.nextLine();
                    if (((Admin) library.getUser()).verify(password))
                        break;
                    else if (i == 2) {
                        System.out.println("3 incorrect password attempts...");
                        library.setUser(null);
                        return;
                    } else
                        System.out.println("Invalid password. Try again.");
                }

            // get user id from command
            int userID = Integer.parseInt(command.substring(18));

            if (library.removeUser(userID) == 0)
                throw new IllegalArgumentException("User with " + userID + " not found!");

            System.out.printf("Account with ID = %d deleted successfully.%n", userID);
        } else if (command.matches("lib\\sadd\\sbook\\s\"?[^\"]*\"?\\s\"?[^\"]*\"?\\s?(\"?[^\"]*\"?)?")) {
            // command lib add book
            // if he didn't admin user yet throw exception
            if (library.getUser() instanceof Admin) {
                String title, author;

                // delete lib add book from command
                command = command.replaceFirst("lib\\sadd\\sbook\\s", "");

                // find the name of the book
                if (command.startsWith("\"")) {
                    title = command.substring(1, command.indexOf("\"", 1));

                    // delete title of the book from command
                    command = command.replaceFirst("\"" + title + "\" ", "");
                } else {
                    title = command.substring(0, command.indexOf(" "));

                    // delete title of the book from command
                    command = command.replaceFirst(title + " ", "");
                }

                // find the author of the book
                if (!command.contains(" ")) {
                    author = command;
                    command = "";
                } else if (command.startsWith("\"")) {
                    author = command.substring(1, command.indexOf("\"", 1));

                    // delete author of the book from command
                    command = command.replaceFirst("\"" + author + "\" ", "");
                } else {
                    author = command.substring(0, command.indexOf(" "));

                    // delete author of the book from command
                    command = command.replaceFirst(author + " ", "");
                }

                // if command doesn't have description
                if (command.equals("")) {
                    Book book = new Book(title.trim(), author.trim());
                    int bookID = library.addBook(book);
                    if (bookID != 0)
                        System.out.printf("Book %s with ID = %d added to library.%n", book.getTitle(),
                                bookID);
                    return;
                }

                String description;

                // find the desciption of the book
                if (command.startsWith("\"")) {
                    description = command.substring(1, command.indexOf("\"", 1));

                    // delete description of the book from command
                    command = command.replaceFirst("\"" + description + "\"", "");
                } else if (command.contains(" "))
                    throw new IllegalArgumentException(
                            "You should use \" character to split string with space");
                else {
                    description = command;

                    // delete description of the book from command
                    command = "";
                }

                // if command has too many argument
                if (!command.equals(""))
                    throw new IllegalArgumentException(
                            "You should use \" character to split string with space");

                Book book = new Book(title.trim(), author.trim(), description.trim());
                int bookID = library.addBook(book);
                if (bookID != 0)
                    System.out.printf("Book %s with ID = %d added to library.%n", book.getTitle(),
                            bookID);
            } else
                throw new NoPermissionException("You don't have permission to add books!");
        } else if (command.matches("lib\\srent\\s.+")) {
            if (library.getUser() == null)
                System.out.println("You should first login to rent a book.");
            else if (library.getUser() instanceof Admin)
                System.out.println("You are admin!You can't rent a book:)");
            else {
                String bookName = command.substring(8).trim();
                if (bookName.startsWith("\""))
                    bookName = bookName.substring(1, bookName.length() - 1);

                ArrayList<Book> books = library.searchBook(bookName);
                Rent rent = null;

                if (books.size() == 0) {
                    System.out.printf("book %s not found or not available!%n", bookName);
                    return;
                } else if (((NormalUser) library.getUser()).getRentBooks().size() >= 5) {
                    System.out.println("You have already rented 5 books and you can no longer " +
                            "rent a book");
                    return;
                } else if (books.size() == 1)
                    rent = new Rent(books.get(0), (NormalUser) library.getUser());
                else {
                    System.out.println("Which book do you want?");

                    // show selections
                    int numberOfRows = 1;
                    for (Book book : books) {
                        System.out.printf("%d. ID = %d, Author = %d", numberOfRows,
                                book.getUniqueID(), book.getAuthor());
                        numberOfRows++;
                    }

                    // select choice
                    int choice = 1;
                    for (int i = 0; i < 3; i++) {
                        if (library.getUser() == null)
                            System.out.print(">>> ");
                        else if (library.getUser() instanceof Admin)
                            System.out.print("Admin> ");
                        else
                            System.out.printf("%s> ", library.getUser().getName());
                        choice = input.nextInt();
                        if (choice > numberOfRows) {
                            if (i == 2) {
                                System.out.println("3 invalid choice...");
                                return;
                            }
                            System.out.print("Invalid choice. please try again.");
                        } else
                            break;
                    }

                    // create rent class
                    rent = new Rent(books.get(choice - 1), (NormalUser) library.getUser());
                }
                int rentalID = ((NormalUser) library.getUser()).rentBook(rent);
                if (rentalID == 0)
                    return;
                System.out.println("You rented this book successfully.");
                System.out.printf("rental ID = %d%n", rentalID);
            }
        } else if (command.matches("lib\\sexit")) {
            //command lib exit
            System.out.print("Bye.");
            System.exit(0);
        } else
            throw new IllegalArgumentException();
    }
}
