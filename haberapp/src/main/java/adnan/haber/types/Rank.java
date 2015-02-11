package adnan.haber.types;

import adnan.haber.util.Debug;

/**
 * Created by Adnan on 25.1.2015..
 */
public enum Rank {
    Guest,
    Admin,
    Adnan,
    Enil,
    Berina,
    Mathilda,
    Moderator,
    Memi,
    User,
    Alma,
    Lamija,
    Merima;

    public static Rank fromString(String role) {
        if ( role.equals("moderator") ) return Moderator;
        if ( role.equals("participant") ) return User;

        Debug.log("Unknown user rank! " + role);
        return Guest;
    }

    public int toInt() {
        if ( this == Guest ) return 0;
        if ( this == User ) return 100;
        if ( this == Merima ) return 105;
        if ( this == Alma ) return 110;
        if ( this == Berina ) return 135;
        if ( this == Lamija ) return 140;
        if ( this == Mathilda ) return 150;
        if ( this == Memi ) return 160;
        if ( this == Enil ) return 170;
        if ( this == Adnan ) return 200;
        if ( this == Moderator ) return 300;
        if ( this == Admin ) return 400;
        Debug.log("This shouldnt happen! Unknown rank in toInt()! " + this.toString());
        return -1;
    }
}
