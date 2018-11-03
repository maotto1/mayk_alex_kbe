package de.htw.ai.kbe.runmerunner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class RunMeFinder {

    private final Class clazz;
    private final String fileName;

    private StringBuilder toWrite;

    private List<Method> methodsWithRunMe = new ArrayList<>();
    private List<Method> methodsWithOutRunMe = new ArrayList<>();
    private HashMap<String, String> methodsGeneratingExceptions = new HashMap<>();

    public RunMeFinder(Class clazz, String fileName) {
        this.clazz = clazz;
        this.fileName = fileName;
    }

    public void execute() {
        checkForRunMeAnnotatedMethods();
        invokeClass();
        writeOutputToFile(methodsWithRunMe, methodsWithOutRunMe, methodsGeneratingExceptions);
    }

    private void checkForRunMeAnnotatedMethods() {
        Stream.of(clazz.getDeclaredMethods())
                .forEach(method -> {
                            method.setAccessible(true);

                            boolean flag = true;
                            for (Annotation annotation : method.getAnnotations()) {
                                if (annotation.annotationType().getName().equals(RunMe.class.getName())) {
                                    methodsWithRunMe.add(method);
                                    flag = false;
                                    break;
                                }
                            }
                            if (flag) {
                                methodsWithOutRunMe.add(method);
                            }
                        }
                );
    }

    private void invokeClass() {
        Object object = instantiateClass();
        methodsWithRunMe.forEach(method -> {
            try {
                method.invoke(object);
            } catch (Exception e) {
                methodsGeneratingExceptions.put(method.getName(), e.toString());
            }
        });
    }

    private Object instantiateClass() {
        try {
            return clazz.newInstance();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeOutputToFile
            (List<Method> methodsWithRunMe, List<Method> methodsWithOutRunMe, HashMap<String, String> methodsGeneratingExceptions) {
        toWrite = new StringBuilder();

        toWrite.append("Methods without @RunMe:\n");
        methodsWithOutRunMe.forEach(method ->
                toWrite
                        .append("\t")
                        .append(method.getName())
                        .append("\n")
        );

        toWrite.append("\n");

        toWrite.append("Methods with @RunMe:\n");
        methodsWithRunMe.forEach(method ->
                toWrite
                        .append("\t")
                        .append(method.getName())
                        .append("\n")
        );

        toWrite.append("\n");

        toWrite.append("Non-invokable methods:\n");
        methodsGeneratingExceptions.forEach(
                (method, exception) ->
                        toWrite
                                .append("\t")
                                .append(method)
                                .append(": ")
                                .append(exception)
                                .append("\n")
        );

        writeToFile();
    }

    private void writeToFile() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("report.txt"))) {
            writer.write(toWrite.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Method> getMethodsWithRunMe() {
        return new ArrayList<>(methodsWithRunMe);
    }

    public List<Method> getMethodsWithOutRunMe() {
        return new ArrayList<>(methodsWithOutRunMe);
    }

    public HashMap<String, String> getMethodsGeneratingExceptions() {
        return new HashMap<>(methodsGeneratingExceptions);
    }
}