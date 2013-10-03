This is some old PHP code in which I'm using as a reference for the verify interface logic

<?php

class Esb_Client {

    private $esbmsg;
    private $log;
    private $config;
    private $backendCacheOptions;
    private $frontendCacheOptions;

    public function __construct() {
        Zend_Registry::get('logger')->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        $this->log = Zend_Registry::get('logger');
        $this->config = Zend_Registry::get('config');
        $this->backendCacheOptions = array(
            'cache_dir' => '/tmp/'
        );

        $this->frontendCacheOptions = array('automatic_serialization' => true);
        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
    }

    public function callService($name, $message, $config = array()) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        if (is_object($message)) {
            $this->log->debug('message is an object converting to array');
            $message = $this->object_to_array($message);
        }

        if (!isset($config['url'])) {
            $url = 'http://localhost:10088/...framework/esb';
            if (isset($this->config->esb_config->endpoint_url)) {
                $url = $this->config->esb_config->endpoint_url;
                $this->log->debug("url from ini file = \"$url\"");
            } else {
                $this->log->warn("URL is not set in application.ini defaulting to \"$url\"");
            }
            $this->log->debug("Setting config['url'] = \"$url\"");
            $config['url'] = $url;
        }

        $this->log->debug('message = "' . print_r($message, true) . '"');
        $service = array_merge(array('service' => array('name' => $name)), $message);
        $service = $this->array_to_object($service);
        $this->log->debug('calling callServiceWithEsbMsg with service = "' . print_r($service, true) . '" and config = "' . print_r($config, true) . '"');
        $service = $this->callServiceWithEsbMsg($service, $config);
        $this->log->debug('returning service = "' . print_r($service, true));
        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
        return $service;
    }

    protected function object_to_array($obj) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        $arrObj = is_object($obj) ? get_object_vars($obj) : $obj;
        foreach ($arrObj as $key => $val) {
            $val = (is_array($val) || is_object($val)) ? $this->object_to_array($val) : $val;
            $arr[$key] = $val;
        }
        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
        return $arr;
    }

    protected function array_to_object($array) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        $obj = new stdClass;
        foreach ($array as $k => $v) {
            if (is_array($v)) {
                $obj->{$k} = $this->array_to_object($v);
            } else {
                $obj->{$k} = $v;
            }
        }
        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
        return $obj;
    }

    public function callServiceWithEsbMsg($esbmsg, $config = array()) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        $this->log->debug('esbmsg = "' . print_r($esbmsg, true) . '" and config = "' . print_r($config, true) . '"');

        if (isset($config['url'])) {
            $url = $config['url'];
            $this->log->debug("url is set and call remote esb with url = \"$url\"");
            $config = array_merge(array('timeout' => 120), $config);
            $this->log->debug('creating new Zend_Http_Client with the url and config = "' . print_r($config, true) . '"');
            $client = new Zend_Http_Client($url, $config);
            $client->setMethod(Zend_Http_Client::POST);
            $encoded = json_encode($esbmsg);
            $client->setRawData($encoded, 'application/json');
            $this->log->debug('Sending POST to url with "application/json" and esbmsg = "' . $encoded . '"');
            $body = $client->request()->getBody();
            $this->log->debug('HTTP Status: ' . $client->request()->getStatus() . ' and body = "' . $body . '"');
            $decoded = json_decode($body);
            $client->getAdapter()->close();
            $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
            return $decoded;
        } else {
            $this->log->debug('PHP Esb Engine started. esbmsg = ' . print_r($esbmsg, true));

            $this->esbmsg = $esbmsg;

            // get the service name
            if (!isset($esbmsg->service)) {
                $errorMsg = "service attribute is required";
                $this->log->err($errorMsg);
                $this->throwException(new _Esb_Exception('com.esb.RegistryException', $errorMsg));
                $this->log->debug('returning esbmsg = "' . print_r($esbmsg, true) . '"');
                return $esbmsg;
            }

            if (!isset($esbmsg->service->name)) {
                $errorMsg = "service name attribute is required";
                $this->log->err($errorMsg);
                $this->throwException(new _Esb_Exception('com.esb.RegistryException', $errorMsg));
                $this->log->debug('returning esbmsg = "' . print_r($esbmsg, true) . '"');
                return $esbmsg;
            }

            $serviceName = $esbmsg->service->name;

            $this->log->debug('Getting definition oject');
            $def = $this->getDefinitionFile($serviceName);

            if ($def === false) {
                $errorMsg = "Reading of service definition file failed.";
                $this->log->err($errorMsg);
//                $this->throwException(new _Esb_Exception('com.esb.RegistryException', $errorMsg));
                $this->log->debug('returning esbmsg = "' . print_r($esbmsg, true) . '"');
                return $esbmsg;
            }

            // check to see if 'interface' exists and verify esbmsg from it
            $verified = true;

            if (isset($def->interface)) {
                $this->log->debug('Interface has been provided.  Verifying esbmsg with it.');
                $verified = $this->verifyInterface($esbmsg, $def->interface);
            }

            if ($verified) {
                // get the actions array
                $this->log->debug('looping actions');
                foreach ($def->actions as $action) {
                    $this->log->debug('processing action = ' . print_r($action, true));

                    if (isset($action->properties)) {
                        $esbprop = $action->properties;
                    } else {
                        $esbprop = new stdClass();
                    }

                    $this->log->debug("esbmsg = \"" . print_r($esbmsg, true) . "\"\nesbprop = \"" . print_r($esbprop, true) . "\"");

                    if (isset($action->php)) {
                        $this->log->debug('This is an PHP Action.  Calling callPhpActionFile');
                        try {
                            $this->callPhpActionFile($esbmsg, $esbprop, $action, $this->log);
                            $this->log->debug('returned from calling callPhpActionFile');
                        } catch (Exception $e) {
                            $errormessage = 'PHP Action call failed. ' . $e->getMessage();
                            $this->log->err($errormessage);
                            $this->throwException(new _Esb_Exception('com.esb.ActionCallException', $errormessage));
                        }
                        $this->log->debug('Finished processing PHP Action');
                    } elseif (isset($action->class)) {
                        $this->log->debug('This is a Java Action Call');
                        $class = $action->class;
                        try {
                            $this->log->debug('Calling Java Action');
                            $esbmsg = $this->callJavaAction($class, $esbmsg, $esbprop, $config);
                        } catch (Exception $e) {
                            $errormessage = 'Java call failed. ' . $e->getMessage();
                            $this->log->err($errormessage);
                            $this->throwException(new _Esb_Exception('com.esb.ActionCallException', $errormessage));
                        }
                        $this->log->debug('Finished processing Java Action');
                    } elseif (isset($action->pgm)) {
                        $this->log->debug('This is an iSeries Program Call');
                        $program = $action->pgm;
                        $message = json_encode($esbmsg);
                        $properties = json_encode($esbprop);
                        $this->log->debug("program = \"$program\"");
                        try {
                            if (!isset($pgmstmt)) {
                                $this->log->debug('building db statement');
                                $this->log->debug('getting database connection');
                                $conn = $this->getConnection();
                                $pgmstmt = db2_prepare($conn, 'call esbcallpgm(?, ?, ?)');
                                db2_bind_param($pgmstmt, 1, 'message', DB2_PARAM_INOUT);
                                db2_bind_param($pgmstmt, 2, 'properties', DB2_PARAM_IN);
                                db2_bind_param($pgmstmt, 3, 'program', DB2_PARAM_IN);
                            }
                            $this->log->debug('Calling Program Action');
                            if (!db2_execute($pgmstmt)) {
                                $errormessage = 'Program Action call failed.  SQLTATE = ' . db2_stmt_error() . ': ' . db2_stmt_errormsg();
                                $this->log->err($errormessage);
                                $this->throwException(new _Esb_Exception('com.esb.ActionCallException', $errormessage));
                            } else {
                                $this->log->debug("call succeded. message = \"$message\"");
                                $esbmsg = json_decode($message);
                                $this->log->debug('decoded esbmsg = ' . print_r($esbmsg, true));
                            }
                        } catch (Exception $e) {
                            $errormessage = 'Program Action call failed. ' . $e->getMessage();
                            $this->log->err($errormessage);
                            $this->throwException(new _Esb_Exception('com.esb.ActionCallException', $errormessage));
                        }
                        $this->log->debug('Finished processing Program Action');
                    } elseif (isset($action->srvpgm)) {
                        $this->log->debug('This is a Service Program Action Call');
                        $srvpgm = $action->srvpgm;
                        $lib = '*LIBL';
                        if (isset($action->lib)) {
                            $lib = $action->lib;
                        }
                        $procedure = $action->procedure;
                        $message = json_encode($esbmsg);
                        $properties = json_encode($esbprop);
                        $this->log->debug("service program = \"$lib/$srvpgm($procedure)\"");
                        try {
                            if (!isset($procstmt)) {
                                $this->log->debug('building db statement');
                                $this->log->debug('getting database connection');
                                $conn = $this->getConnection();
                                $procstmt = db2_prepare($conn, 'call esbcallproc(?, ?, ?, ?, ?)');
                                db2_bind_param($procstmt, 1, 'message', DB2_PARAM_INOUT);
                                db2_bind_param($procstmt, 2, 'properties', DB2_PARAM_IN);
                                db2_bind_param($procstmt, 3, 'lib', DB2_PARAM_IN);
                                db2_bind_param($procstmt, 4, 'srvpgm', DB2_PARAM_IN);
                                db2_bind_param($procstmt, 5, 'procedure', DB2_PARAM_IN);
                            }
                            if (!db2_execute($procstmt)) {
                                $errormessage = 'Service Program Action call failed.  SQLTATE = ' . db2_stmt_error() . ': ' . db2_stmt_errormsg();
                                $this->log->err($errormessage);
                                $this->throwException(new _Esb_Exception('com.esb.Exception', $errormessage));
                            } else {
                                $this->log->debug("call succeded. message = \"$message\"");
                                $esbmsg = json_decode($message);
                                $this->log->debug('decoded esbmsg = ' . print_r($esbmsg, true));
                            }
                        } catch (Exception $e) {
                            $errormessage = 'Service Program Action call failed. ' . $e->getMessage();
                            $this->log->err($errormessage);
                            $this->throwException(new _Esb_Exception('com.esb.ActionCallException', $errormessage));
                        }
                        $this->log->debug('Finished processing Service Program Action');
                    }

                    // check for attribute 'exception' and quit
                    if (isset($esbmsg->exception)) {
                        $this->log->err('An exception was found.  Ending service.  Exception is: ' . print_r($esbmsg->exception, true));
                        break;
                    }
                    $this->log->debug('End of processing current action');
                }
            }

            $this->log->debug('returning esbmsg = "' . print_r($esbmsg, true) . '"');
            $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
            return $esbmsg;
        }
    }

    private function getDefinitionFile($serviceName) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        $deffile = false;
        $paths = array(OPT_ROOT . '//UserData/esb/definitions', OPT_ROOT . '//ProdData/esb/definitions');

        if (isset($this->config->esb_config->definition->path)) {
            $iniPaths = $this->config->esb_config->definition->path;
            $this->log->debug('paths have been set in config file. ' . print_r($iniPaths, true));

            if (is_object($iniPaths)) {
                $iniPaths = $iniPaths->toArray();
            } else {
                $iniPaths = array($iniPaths);
            }
        } else {
            $this->log->debug('Paths have not been set in the config file.  Using defaults.');
        }

        $paths = array_merge($paths, $iniPaths);

        $this->log->debug('Reading files and first valid wins. ' . print_r($paths, true));

        foreach ($paths as $path) {
            $filename = "$path/$serviceName.json";
            $this->log->debug("Testing $filename");
            if (is_file($filename)) {
                $this->log->debug("\"$filename\" wins.  Trying to read.");
                $md5Filename = md5($filename);

                // get the location of the httpd.conf to the timestamp for the cache
                // in case the OPT_ROOT has changed in it.
                $httpd_conf_file = getenv('HTTPD_CONF_FILE') ? getenv('HTTPD_CONF_FILE') : '/www/zendconf/conf/httpd.conf';

                $this->frontendCacheOptions['master_files'] = array($filename, $httpd_conf_file);
                $cache = Zend_Cache::factory('File', 'File', $this->frontendCacheOptions, $this->backendCacheOptions);

                $this->log->debug("md5 filename (key to cache) : $md5Filename");

                if (!($cache->test($md5Filename))) {
                    $this->log->debug('build json def file into cache.  Reading file.');

                    // this allows the json file to have embedded php code
                    ob_start();
                    include $filename;
                    $deffile = ob_get_clean();

                    $this->log->debug('read from file: ' . print_r($deffile, true));
                    if ($deffile != false) {
                        $this->log->debug("Definition file contents = \"$deffile\"");
                        $deffile = json_decode($deffile);
                        if ($deffile != null) {
                            $this->log->debug('Saving decoded JSON object to cache');
                            $cache->save($deffile);
                        } else {
                            $this->log->err("Error decoding to an JSON object. File = \"$filename\"");
                            $errorMsg = "Error decoding to an JSON object.";
                            $this->log->err($errorMsg);
                            $this->throwException(new _Esb_Exception('com.esb.RegistryException', $errorMsg));
                            $deffile = false;
                        }
                    }
                    $this->log->debug("End of reading files.");
                } else {
                    $this->log->debug("loading definition object from cache: $md5Filename");
                    $deffile = $cache->load($md5Filename);
                    $this->log->debug('read from cache: ' . print_r($deffile, true));
                }

                break;
            }
        }

        if ($deffile === false) {
            $errorMsg = "Definition file could not be found or read.";
            $this->log->err($errorMsg);
            $this->throwException(new _Esb_Exception('com.esb.RegistryException', $errorMsg));
        }

        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
        return $deffile;
    }

    public function callRawService($rawBody, $config = array()) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        $esbmsg = json_decode($rawBody);
        $esbmsg = $this->callServiceWithEsbMsg($esbmsg, $config);
        $encoded = json_encode($esbmsg);
        return $encoded;
    }

    private function callPhpActionFile(&$esbmsg, &$esbprop, $action, &$log) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        if (file_exists($action->php) && is_file($action->php)) {
            include $action->php;
        } else {
            $errormessage = 'PHP Action call failed. Cannot load action file.';
            $this->log->err($errormessage);
            $this->throwException(new _Esb_Exception('com.esb.ActionCallException', $errormessage));
        }
        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
    }

    private function getConnection() {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        $database = "";
        $username = "";
        $password = "";
        $options = array('i5_libl' => 'GSFL2K GSAPI GSXA2K GSGS2K GSAR2K GSRF2K GSNETCOE', 'i5_naming' => DB2_I5_NAMING_ON, 'i5_date_fmt' => DB2_I5_FMT_ISO);
        $this->log->debug("Set defaults of db connection to:  database = \"$database\", username = \"$username\", password = \"$password\", and options = " . print_r($options, true));

        if (isset($this->config->esb_config->db->database)) {
            $this->log->debug('esb_config->db->database exists = "' . $this->config->esb_config->db->database . '"');
            $database = $this->config->esb_config->db->database;
        }

        if (isset($this->config->esb_config->db->username)) {
            $this->log->debug('esb_config->db->username exists = "' . $this->config->esb_config->db->username . '"');
            $username = $this->config->esb_config->db->username;
        }

        if (isset($this->config->esb_config->db->password)) {
            $this->log->debug('esb_config->db->password exists = "' . $this->config->esb_config->db->password . '"');
            $password = $this->config->esb_config->db->password;
        }

        if (isset($this->config->esb_config->db->options)) {
            $optionFromConfig = $this->config->esb_config->db->options->toArray();
            $this->log->debug('esb_config->db->options exists = ' . var_export($optionFromConfig, true));
            $options = array_merge($options, $optionFromConfig);
        }

        $this->log->debug("Final settings of db connection are: database = \"$database\", username = \"$username\", password = \"$password\", and options = " . var_export($options, true));

        $connection = db2_pconnect($database, $username, $password, $options);

        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
        return $connection;
    }

    /**
     * Adds an EsbExction object to the Esb Message 'exception' array
     * @param EsbException $esbException
     */
    private function throwException($esbException) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        // is the exception in the message
        if (isset($this->esbmsg->exception)) {
            $exceptions = $this->esbmsg->exception;
        } else {
            // exception is not in the message.  Create a new one
            $exceptions = array();
        }
        // add the exception object to the array
        $exceptions[] = $esbException;
        $this->esbmsg->exception = $exceptions;
        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
    }

    private function verifyInterface($esbmsg, $interface) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        $rc = true;
        if (!is_array($interface)) {
            $errormessage = "Service Interface is not an array.";
            $this->log->err($errormessage);
            $this->throwException(new _Esb_Exception('com.esb.ServiceInterfaceException', $errormessage));
            $rc = false;
        } else {
            foreach ($interface as $param) {
                if (isset($param->name)) {
                    $name = trim($param->name);
                    if (strlen($name) > 0) {
                        $required = true;
                        if (isset($param->required)) {
                            $required = $param->required ? true : false;
                        }
                        if ($required) {
                            if (!isset($esbmsg->{$name})) {
                                // property in esbmsg is not found and is required
                                $errormessage = "Property \"$name\" is required";
                                $this->log->err($errormessage);
                                $this->throwException(new _Esb_Exception('com.esb.ServiceInterfaceException', $errormessage));
                                $rc = false;
                                break;
                            }
                        }
                        // check for default and apply
                        if (!$required && isset($param->default)) {
                            $esbmsg->{$name} = $param->default;
                        }
                        // check type
                        if (isset($param->type)) {
                            if ($this->array_in_array(is_array($param->type) ? $param->type : array($param->type), array('string', 'numeric', 'bool', 'array', 'object'))) {
                                $runtimeType = $this->getRuntimeType($esbmsg->{$name});
                                if (!$this->isValidRuntimeType($runtimeType, $param->type)) {
                                    $errormessage = "Property \"$name\" value is of the wrong type.";
                                    $this->log->err($errormessage);
                                    $this->throwException(new _Esb_Exception('com.esb.ServiceInterfaceException', $errormessage));
                                    $rc = false;
                                    break;
                                }
                            } else {
                                $errormessage = "Interface syntax error: Property type specified for property \"$name\" is invalid.";
                                $this->log->err($errormessage);
                                $this->throwException(new _Esb_Exception('com.esb.ServiceInterfaceException', $errormessage));
                                $rc = false;
                                break;
                            }
                        }
                    } else {
                        // param name is blank
                        $errormessage = "Interface syntax error: Property \"name\" is a blank value.";
                        $this->log->err($errormessage);
                        $this->throwException(new _Esb_Exception('com.esb.ServiceInterfaceException', $errormessage));
                        $rc = false;
                        break;
                    }
                } else {
                    // param name not found
                    $errormessage = "Interface syntax error: Interface property \"name\" is required.";
                    $this->log->err($errormessage);
                    $this->throwException(new _Esb_Exception('com.esb.ServiceInterfaceException', $errormessage));
                    $rc = false;
                    break;
                }
            }
        }

        $this->log->debug("verified = $rc");
        return $rc;
        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
    }

    private function array_in_array($needles, $haystack) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        $rc = false;
        foreach ($needles as $needle) {
            if (in_array($needle, $haystack)) {
                $rc = true;
                break;
                ;
            }
        }

        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
        return $rc;
    }

    private function getRuntimeType($value) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        if (is_string($value)) {
            $type = 'string';
        } elseif (is_numeric($value)) {
            $type = 'numeric';
        } elseif (is_bool($value)) {
            $type = 'bool';
        } elseif (is_array($value)) {
            $type = 'array';
        } elseif (is_object($type)) {
            $type = 'object';
        }
        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
        return $type;
    }

    private function isValidRuntimeType($runtimeType, $type) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        if (!is_array($type)) {
            $type = array($type);
        }

        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
        return in_array($runtimeType, $type);
    }

    private function callJavaAction($className, $esbMsg, $esbProp, $config = array()) {
        $this->log->debug('Entering ' . __FILE__ . ' ' . __METHOD__);
        if (!isset($config['url'])) {
            $url = 'http://localhost:10088/framework/esb';
            if (isset($this->config->esb_config->endpoint_url)) {
                $url = $this->config->esb_config->endpoint_url;
                $this->log->debug("url from ini file = \"$url\"");
            } else {
                $this->log->warn("URL is not set in application.ini defaulting to \"$url\"");
            }
            $this->log->debug("Setting config['url'] = \"$url\"");
            $config['url'] = $url;
        }
        $url = $config['url'] . "/callJavaAction";;
        $this->log->debug("url is set and call remote esb java action with url = \"$url\"");
        $config = array_merge(array('timeout' => 120), $config);
        $this->log->debug('creating new Zend_Http_Client with the url and config = "' . print_r($config, true) . '"');
        $client = new Zend_Http_Client($url, $config);
        $client->setMethod(Zend_Http_Client::POST);
        $javaActionObj = new stdClass;
        $javaActionObj->className = $className;
        $javaActionObj->esbmsg = $esbMsg;
        $javaActionObj->esbprop = $esbProp;
        $encoded = json_encode($javaActionObj);
        $client->setRawData($encoded, 'application/json');
        $this->log->debug('Sending POST to url with "application/json" and esbmsg = "' . $encoded . '"');
        $body = $client->request()->getBody();
        $this->log->debug('HTTP Status: ' . $client->request()->getStatus() . ' and body = "' . $body . '"');
        $decoded = json_decode($body);
        $client->getAdapter()->close();
        $this->log->debug('Leaving ' . __FILE__ . ' ' . __METHOD__);
        return $decoded;
    }
}