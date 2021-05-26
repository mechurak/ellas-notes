package com.shimnssso.headonenglish.room

class FakeData {
    companion object {
        val DEFAULT_SUBJECT = DatabaseSubject(
            0,
            "정면돌파 스피킹",
            0,
            "https://home.ebse.co.kr/10mins_lee2/",
            "1veQzV0fyYHO_4Lu2l33ZRXbjy47_q8EI1nwVAQXJcVQ",
            true
        )

        val DEFAULT_LECTURE = DatabaseLecture(
            subjectId = 0,
            date = "2021-05-12",
            title = "발음 강세 Unit 553. 체중",
            category = "Maintaining Our Health",
            remoteUrl = "https://m4strssl.ebse.co.kr/2021/er2017h0spe01zz/1m/20210512_063000_6b6f5fd4_m10.mp4",
            localUrl = null,
            link1 = null,
            link2 = null
        )
    }
}