package com.prateektimer.util.model


import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
class Diary : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var owner_id: String = ""
    var mood: String = Mood.Neutral.name
    var title: String = ""
    var description: String = ""
    var images: RealmList<String> = realmListOf()
    var date: RealmInstant = RealmInstant.now()//Instant.now().toRealmInstant()
}