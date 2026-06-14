package com.nexa.mocking;

public interface EmailService {

    boolean envoyerEmail(String destinataire, String sujet, String contenu);
}
