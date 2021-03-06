package com.shimnssso.headonenglish.room

class FakeData {
    companion object {
        val DEFAULT_SUBJECTS = listOf(
            DatabaseSubject(
                subjectId = 1,
                title = "정면돌파 스피킹 template",
                sheetId = "1veQzV0fyYHO_4Lu2l33ZRXbjy47_q8EI1nwVAQXJcVQ",
                lastUpdateTime = 0L,
                description = "하루에 한 표현씩 Speed 실전 스피킹!",
                link = "https://home.ebse.co.kr/10mins_lee2/main",
                image = "https://static.ebs.co.kr/images/public/courses/2021/02/19/20/ER2017H0SPE01ZZ/8f8797ce-8085-4a0f-9681-4df159c3de17.jpg",
            ),
            DatabaseSubject(
                subjectId = 2,
                title = "입트영 60 (일상생활편) template",
                sheetId = "1GeK1Kz8GycGMYviq52sqV3-WKoI8Gw7llSOvJekp01s",
                lastUpdateTime = 0L,
                description = "영어가 더 유창해지는 <입이 트이는 영어> 베스트 컬렉션",
                link = "https://book.naver.com/bookdb/book_detail.nhn?bid=16744854",
                image = "https://image.kyobobook.co.kr/images/book/xlarge/937/x9788954753937.jpg"
            ),
            DatabaseSubject(
                subjectId = 3,
                title = "귀트영 template",
                sheetId = "1FaIhdmMIa77CoZCkhVly1rPrSdRs6Fg3ZIR5ofGu7hw",
                lastUpdateTime = 0L,
                description = "이현석&안젤라와 함께하는 대한민국 대표 영어 리스닝 프로그램",
                link = "https://home.ebs.co.kr/listene/main",
                image = "https://image.kyobobook.co.kr/images/book/xlarge/697/x3904000048697.jpg"
            ),
        )

        val DEFAULT_GLOBAL = DatabaseGlobal(
            id = 1,
            subjectId = 1,
        )

        val DEFAULT_LECTURE = DatabaseLecture(
            subjectId = 1,
            date = "2021-05-12",
            title = "발음 강세 Unit 553. 체중",
            category = "Maintaining Our Health",
            remoteUrl = null,
            localUrl = null,
            link1 = null,
            link2 = null
        )
    }
}