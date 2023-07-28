package com.naoido

import java.util.logging.Logger

//logger
val Any.logger: Logger get() = Logger.getLogger(this.javaClass.toString());

fun main() {
    JDACore.build();
}
