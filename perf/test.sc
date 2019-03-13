/**
 * Perf tester
 *
 * Deploys a Cromwell helm chart to a cluster
 *
 * Runs a gatling script
 * record
 *   gatling conf
 *   simulation log
 *
 * Tears down the helm chart
 */

import $ivy.`io.gatling:gatling-core:3.0.3`

/*
 Sample gatling data
================================================================================
---- Global Information --------------------------------------------------------
> request count                                         13 (OK=13     KO=0     )
> min response time                                    104 (OK=104    KO=-     )
> max response time                                    412 (OK=412    KO=-     )
> mean response time                                   191 (OK=191    KO=-     )
> std deviation                                         76 (OK=76     KO=-     )
> response time 50th percentile                        184 (OK=184    KO=-     )
> response time 75th percentile                        208 (OK=208    KO=-     )
> response time 95th percentile                        317 (OK=317    KO=-     )
> response time 99th percentile                        393 (OK=393    KO=-     )
> mean requests/sec                                   0.52 (OK=0.52   KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                            13 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)

*/

