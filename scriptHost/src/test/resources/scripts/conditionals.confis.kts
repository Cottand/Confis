val ana by party
val bob by party


val cake by thing
val eat by action


// ana may eat cake only if bob has eaten the cookie!

bob may { eat(cookie) }

ana may { eat(cake) } asLongAs {
    bob did { eat(cookie) }
}

