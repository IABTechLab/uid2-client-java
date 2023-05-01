package com.uid2.client;

import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public final class MappingRequest {
    private List<String> email;

    @SerializedName("email_hash")
    private List<String> emailHash;

    private List<String> phone;

    @SerializedName("phone_hash")
    private List<String> phoneHash;

    public List<String> getEmail() {
        return email;
    }

    public void setEmail(List<String> email) {
        this.email = email;
    }

    public MappingRequest email(List<String> email) {
        this.email = email;
        return this;
    }

    public MappingRequest email(String ...email) {
        this.email = Arrays.asList(email);
        return this;
    }

    public List<String> getEmailHash() {
        return emailHash;
    }

    public void setEmailHash(List<String> emailHash) {
        this.emailHash = emailHash;
    }

    public MappingRequest emailhash(List<String> emailHash){
        this.emailHash = emailHash;
        return this;
    }

    public MappingRequest emailHash(String ...emailHash) {
        this.emailHash = Arrays.asList(emailHash);
        return this;
    }

    public List<String> getPhone() {
        return phone;
    }

    public void setPhone(List<String> phone) {
        this.phone = phone;
    }

    public MappingRequest phone(List<String> phone) {
        this.phone = phone;
        return this;
    }

    public MappingRequest phone(String... phone) {
        this.phone = Arrays.asList(phone);
        return this;
    }

    public List<String> getPhoneHash() {
        return phoneHash;
    }

    public void setPhoneHash(List<String> phoneHash) {
        this.phoneHash = phoneHash;
    }

    public MappingRequest phoneHash(List<String> phoneHash) {
        this.phoneHash = phoneHash;
        return this;
    }

    public MappingRequest phoneHahs(String... phoneHash) {
        this.phoneHash = Arrays.asList(phoneHash);
        return this;
    }
}
