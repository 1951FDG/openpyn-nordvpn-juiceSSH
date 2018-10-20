package io.github.sdsstudios.nvidiagpumonitor

/*
    private fun createJson0() {
        //an extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
        "https://api.nordvpn.com/server".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    val ex = result.getException()
                }
                is Result.Success -> {
                    operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
                    val jsonObj = JSONObject()
                    val content = result.get().array() //JSONArray
                    for (res in content) {
                        val country = res.getString("domain").take(2)

                        var pass = country.equals(country_code,true)

                        if (!pass) {
                            //continue
                        }

                        pass = when {
                            p2p -> false
                            dedicated -> false
                            double_vpn -> false
                            tor_over_vpn -> false
                            anti_ddos -> false
                            else -> true
                        }

                        if (!pass) {
                            val categories = res.getJSONArray("categories")

                            for (category in categories) {
                                val name = category.getString("name")

                                if (p2p and name.equals("P2P", true)) {
                                    pass = true
                                    break
                                }
                                else if (dedicated and name.equals("Dedicated IP servers", true)) {
                                    pass = true
                                    break
                                }
                                else if (double_vpn and name.equals("Double VPN", true)) {
                                    pass = true
                                    break
                                }
                                else if (tor_over_vpn and name.equals("Obfuscated Servers", true)) {
                                    pass = true
                                    break
                                }
                                else if (anti_ddos and name.equals("Anti DDoS", true)) {
                                    pass = true
                                    break
                                }
                            }
                        }

                        if (!pass) {
                            continue
                        }

                        val location = res.getJSONObject("location")

                        var jsonArr = jsonObj.optJSONArray(location.toString())
                        if (jsonArr == null) {
                            jsonArr = JSONArray()
                            jsonArr.put(res)
                            jsonObj.put(location.toString(), jsonArr)
                        }
                        else {
                            jsonArr.put(res)
                        }
                    }

                    try {
                        val keys = jsonObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = jsonObj.getJSONArray(key)
                            val location = value.getJSONObject(0).getJSONObject("location")
                            val marker = mMap.addMarker(MarkerOptions().position(LatLng(location.getDouble("lat"), location.getDouble("long"))).visible(false))
                            marker.tag = value

                            items.add(marker)
                        }
                    } catch (e: JSONException) {
                        error(e)
                    }

//                    val file = File(this.getExternalFilesDir(null),"output.json")
//                    file.writeText(jsonObj.toString())
//                    debug(file)
                }
            }
        }
    }
*/
