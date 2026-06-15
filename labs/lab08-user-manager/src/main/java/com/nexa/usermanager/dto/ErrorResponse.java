package com.nexa.usermanager.dto;

import java.util.Map;

/**
 * DTO representant une réponse d'erreur structuree conforme au standard
 * RFC 7807 (Problem Details for HTTP APIs).
 *
 * <p>Cette classe est utilisé́e par le {@code GlobalExceptionHandler} pour
 * retourner des reponses d'erreur coherentes et informatives au client.</p>
 *
 * <p>Structure de la réponse :</p>
 * <ul>
 *   <li>{@code status} : code HTTP de l'erreur</li>
 *   <li>{@code title} : titre court decrivant le type d'erreur</li>
 *   <li>{@code detail} : message detaille expliquant la cause de l'erreur</li>
 *   <li>{@code errors} : map optionnelle des erreurs par champ (validation)</li>
 * </ul>
 */
public class ErrorResponse {

    /** Code de statut HTTP de l'erreur (ex: 400, 404, 409). */
    private int status;

    /** Titre court decrivant la nature de l'erreur. */
    private String title;

    /** Message detaille expliquant la cause precise de l'erreur. */
    private String detail;

    /** Map des erreurs de validation par nom de champ (optionnelle). */
    private Map<String, String> errors;

    /**
     * Constructeur pour une erreur simple sans details de validation.
     *
     * @param status le code de statut HTTP
     * @param title  le titre de l'erreur
     * @param detail le message detaille
     */
    public ErrorResponse(int status, String title, String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    /**
     * Constructeur pour une erreur avec details de validation par champ.
     *
     * @param status le code de statut HTTP
     * @param title  le titre de l'erreur
     * @param detail le message detaille
     * @param errors la map des erreurs par nom de champ
     */
    public ErrorResponse(int status, String title, String detail, Map<String, String> errors) {
        this(status, title, detail);
        this.errors = errors;
    }

    /** @return le code de statut HTTP */
    public int getStatus() { return status; }
    /** @param status le code de statut a definir */
    public void setStatus(int status) { this.status = status; }
    /** @return le titre de l'erreur */
    public String getTitle() { return title; }
    /** @param title le titre a definir */
    public void setTitle(String title) { this.title = title; }
    /** @return le message detaille */
    public String getDetail() { return detail; }
    /** @param detail le message a definir */
    public void setDetail(String detail) { this.detail = detail; }
    /** @return la map des erreurs de validation par champ */
    public Map<String, String> getErrors() { return errors; }
    /** @param errors la map des erreurs a definir */
    public void setErrors(Map<String, String> errors) { this.errors = errors; }
}
