package com.example.vetcalls.obj;

public class User {

    public String email;
    public String weight;
    public String race;
    public String allergies;
    public String vaccines;
    public String name;
    public String birthday;
    public Boolean isVet;



    public User(String email, String vaccines, String allergies, String race, String weight, String name, String birthday, Boolean isVet
    ) {
        this.email = email;
        this.vaccines = vaccines;
        this.allergies = allergies;
        this.race = race;
        this.weight = weight;
        this.name = name;
        this.birthday = birthday;
        this.isVet = isVet;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getVaccines() {
        return vaccines;
    }

    public void setVaccines(String vaccines) {
        this.vaccines = vaccines;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public Boolean getVet() {
        return isVet;
    }

    public void setVet(Boolean vet) {
        isVet = false;
    }
}
