package com.finance;

/**
 * Ponto de entrada separado exigido pelo jpackage.
 * O jpackage não aceita diretamente classes que estendem javafx.application.Application
 * como main-class quando o JAR não é modular.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
