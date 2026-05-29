package com.shortsblockerkids.accessibility

internal object ShortVideoTextSignals {
    val shortsIdentifiers = setOf("shorts")
    val reelsIdentifiers = setOf("reel", "reels", "рілс", "рилс")

    val tiktokFeed =
        setOf(
            "for you",
            "para ti",
            "following",
            "siguiendo",
            "für dich",
            "для вас",
            "рекомендації",
            "рекомендации",
            "підписки",
            "подписки",
        )

    val tiktokNavigation =
        setOf(
            "home",
            "inicio",
            "startseite",
            "головна",
            "главная",
            "friends",
            "amigos",
            "freunde",
            "друзі",
            "друзья",
            "inbox",
            "bandeja",
            "posteingang",
            "вхідні",
            "входящие",
            "profile",
            "perfil",
            "profil",
            "профіль",
            "профиль",
        )

    private val like =
        setOf(
            "like",
            "me gusta",
            "gefällt mir",
            "подобається",
            "нравится",
        )

    private val dislike =
        setOf(
            "dislike",
            "не подобається",
            "не нравится",
        )

    private val comments =
        setOf(
            "comments",
            "comment",
            "comentar",
            "kommentieren",
            "коментар",
            "коментарі",
            "коментувати",
            "комментарий",
            "комментарии",
            "комментировать",
        )

    private val share =
        setOf(
            "share",
            "compartir",
            "teilen",
            "поділитися",
            "поширити",
            "поделиться",
        )

    private val send =
        setOf(
            "send",
            "enviar",
            "senden",
            "надіслати",
            "отправить",
        )

    private val save =
        setOf(
            "save",
            "guardar",
            "speichern",
            "зберегти",
            "сохранить",
        )

    private val audio = setOf("audio")

    private val more =
        setOf(
            "more",
            "mehr",
            "більше",
            "еще",
            "ещё",
        )

    private val remix =
        setOf(
            "remix",
            "ремікс",
            "ремикс",
        )

    private val subscribe =
        setOf(
            "subscribe",
            "abonnieren",
            "підписатися",
            "подписаться",
        )

    private val follow =
        setOf(
            "follow",
            "seguir",
            "folgen",
            "стежити",
            "подписаться",
        )

    private val search =
        setOf(
            "search",
            "buscar",
            "suche",
            "пошук",
            "поиск",
        )

    private val settings =
        setOf(
            "settings",
            "ajustes",
            "einstellungen",
            "налаштування",
            "настройки",
        )

    private val subscriptions = setOf("subscriptions")

    val youtubeActions = like + dislike + comments + share + remix + subscribe
    val tiktokActions = like + comments + share + save + more + follow
    val instagramActions = like + comments + share + send + save + audio + more
    val facebookActions = like + comments + share + send + save + more + follow

    val knownContentSignals =
        shortsIdentifiers +
            reelsIdentifiers +
            tiktokFeed +
            tiktokNavigation +
            like +
            dislike +
            comments +
            share +
            send +
            save +
            audio +
            more +
            remix +
            subscribe +
            follow +
            search +
            settings +
            subscriptions
}
