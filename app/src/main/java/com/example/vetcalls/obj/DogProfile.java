package com.example.vetcalls.obj;

/**
 * מחלקה המייצגת פרופיל של כלב באפליקציה
 */
public class DogProfile {
    // שדות בסיסיים
    private String id;                // מזהה ייחודי של הכלב
    private String name;              // שם הכלב
    private String age;               // גיל הכלב
    private String bio;               // ביוגרפיה או תיאור
    private String imageUrl;          // כתובת URL לתמונת הפרופיל
    private String race;              // גזע הכלב
    private String birthday;          // תאריך לידה של הכלב

    // שדות נוספים
    private String weight;            // משקל הכלב
    private String allergies;         // אלרגיות
    private String vaccines;          // חיסונים ותרופות
    private String ownerId;           // מזהה הבעלים
    private String vetId;             // מזהה הווטרינר
    private boolean isCurrent;        // האם זה הכלב הנוכחי המוצג

    /**
     * בנאי ברירת מחדל - נדרש עבור Firestore
     */
    public DogProfile() {
        // דרוש לפיירסטור
    }

    /**
     * בנאי בסיסי
     */
    public DogProfile(String name, String age, String bio, String imageUrl, String race, String birthday) {
        this.name = name;
        this.age = age;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.race = race;
        this.birthday = birthday;
    }

    /**
     * בנאי עם מזהה
     */
    public DogProfile(String id, String name, String age, String bio, String imageUrl, String race, String birthday) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.race = race;
        this.birthday = birthday;
    }

    /**
     * בנאי מורחב עם כל השדות
     */
    public DogProfile(String id, String name, String age, String bio, String imageUrl, String race,
                      String birthday, String weight, String allergies, String vaccines,
                      String ownerId, String vetId) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.race = race;
        this.birthday = birthday;
        this.weight = weight;
        this.allergies = allergies;
        this.vaccines = vaccines;
        this.ownerId = ownerId;
        this.vetId = vetId;
        this.isCurrent = false;
    }

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getVaccines() { return vaccines; }
    public void setVaccines(String vaccines) { this.vaccines = vaccines; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getVetId() { return vetId; }
    public void setVetId(String vetId) { this.vetId = vetId; }

    public boolean isCurrent() { return isCurrent; }
    public void setCurrent(boolean current) { isCurrent = current; }

    /**
     * שיטה ליצירת מחרוזת מייצגת של האובייקט
     */
    @Override
    public String toString() {
        return "DogProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", race='" + race + '\'' +
                ", age='" + age + '\'' +
                '}';
    }

    /**
     * שיטה לבדיקת שוויון בין שני אובייקטים
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DogProfile that = (DogProfile) obj;
        return id != null && id.equals(that.id);
    }

    /**
     * שיטה ליצירת קוד האש
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}