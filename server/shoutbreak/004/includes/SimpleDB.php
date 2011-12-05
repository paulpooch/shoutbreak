<?php
/**
 *
 * Copyright (c) 2010, Dan Myers.
 * Parts copyright (c) 2008, Donovan Schonknecht.
 * Additional functionality by Rich Helms rich@webmasterinresidence.ca
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * This is a modified BSD license (the third clause has been removed).
 * The BSD license may be found here:
 * http://www.opensource.org/licenses/bsd-license.php
 *
 * Amazon SimpleDB is a trademark of Amazon.com, Inc. or its affiliates.
 *
 * SimpleDB is based on Donovan Schonknecht's Amazon S3 PHP class, found here:
 * http://undesigned.org.za/2007/10/22/amazon-s3-php-class
 *
 * For notes on eventual consistency and the use of the ConsistentRead parameter,
 * see http://developer.amazonwebservices.com/connect/entry.jspa?externalID=3572
 *
 */

/**
 * Amazon SimpleDB PHP class
 *
 * @link http://sourceforge.net/projects/php-sdb/
 * version 0.7.2
 *
 */
class SimpleDB {
	private static $__accessKey; // AWS Access key
	private static $__secretKey; // AWS Secret key
	
	// PAP
	private static $handledErrors = array(
		'NoSuchDomain' => 1,
		'ServiceUnavailable' => 1,
		'InternalError' => 1);
	
	public static $useSSL = true;
	public static $verifyHost = 1;
	public static $verifyPeer = 1;
	
	// information related to last request
	public $BoxUsage;
	public $RequestId;
	public $NextToken;
	public $ErrorCode;
	
	
	
	/**
	 * Constructor - if you're not using the class statically
	 *
	 * @param string $accessKey Access key
	 * @param string $secretKey Secret key
	 * @param boolean $useSSL Enable SSL
	 * @return void
	 */
	public function __construct($accessKey = null, $secretKey = null, $useSSL = true) {
		if ($accessKey !== null && $secretKey !== null)
			self::setAuth($accessKey, $secretKey);
		self::$useSSL = $useSSL;
	}
	
	/**
	 * Set AWS access key and secret key
	 *
	 * @param string $accessKey Access key
	 * @param string $secretKey Secret key
	 * @return void
	 */
	public static function setAuth($accessKey, $secretKey) {
		self::$__accessKey = $accessKey;
		self::$__secretKey = $secretKey;
	}
	
	/**
	 * Enable or disable VERIFYHOST for SSL connections
	 * Only has an effect if $useSSL is true
	 *
	 * @param boolean $enable Enable VERIFYHOST
	 * @return void
	 */
	public static function enableVerifyHost($enable = true) {
		self::$verifyHost = ($enable ? 1 : 0);
	}
	
	/**
	 * Enable or disable VERIFYPEER for SSL connections
	 * Only has an effect if $useSSL is true
	 *
	 * @param boolean $enable Enable VERIFYPEER
	 * @return void
	 */
	public static function enableVerifyPeer($enable = true) {
		self::$verifyPeer = ($enable ? 1 : 0);
	}
	
	/**
	 * Create a domain
	 *
	 * @param string $domain The domain to create
	 * @return boolean
	 */
	public function createDomain($domain) {
		SimpleDB::__clearReturns();
		
		$rest = new SimpleDBRequest($domain, 'CreateDomain', 'POST', self::$__accessKey);
		$rest = $rest->getResponse();
		if ($rest->error === false && $rest->code !== 200)
			$rest->error = array('code' => $rest->code, 'message' => 'Unexpected HTTP status');
		if ($rest->error !== false) {
			SimpleDB::__triggerError('createDomain', $rest->error);
			return false;
		}
		
		if (isset($rest->body->ResponseMetadata->RequestId)) {
			$this->RequestId = (string) ($rest->body->ResponseMetadata->RequestId);
		}
		if (isset($rest->body->ResponseMetadata->BoxUsage)) {
			$this->BoxUsage = (string) ($rest->body->ResponseMetadata->BoxUsage);
		}
		
		return true;
	}
	
	/**
	 * Delete a domain
	 *
	 * @param string $domain The domain to delete
	 * @return boolean
	 */
	public function deleteDomain($domain) {
		SimpleDB::__clearReturns();
		
		$rest = new SimpleDBRequest($domain, 'DeleteDomain', 'DELETE', self::$__accessKey);
		$rest = $rest->getResponse();
		if ($rest->error === false && $rest->code !== 200)
			$rest->error = array('code' => $rest->code, 'message' => 'Unexpected HTTP status');
		if ($rest->error !== false) {
			SimpleDB::__triggerError('deleteDomain', $rest->error);
			return false;
		}
		
		if (isset($rest->body->ResponseMetadata->RequestId)) {
			$this->RequestId = (string) ($rest->body->ResponseMetadata->RequestId);
		}
		if (isset($rest->body->ResponseMetadata->BoxUsage)) {
			$this->BoxUsage = (string) ($rest->body->ResponseMetadata->BoxUsage);
		}
		
		return true;
	}
	
	/**
	 * Get a list of domains
	 *
	 * @return array | false
	 */
	public function listDomains() {
		SimpleDB::__clearReturns();
		
		$rest = new SimpleDBRequest('', 'ListDomains', 'GET', self::$__accessKey);
		$rest = $rest->getResponse();
		if ($rest->error === false && $rest->code !== 200)
			$rest->error = array('code' => $rest->code, 'message' => 'Unexpected HTTP status');
		if ($rest->error !== false) {
			SimpleDB::__triggerError('listDomains', $rest->error);
			return false;
		}
		
		$results = array();
		if (! isset($rest->body->ListDomainsResult)) {
			return $results;
		}
		
		foreach ($rest->body->ListDomainsResult->DomainName as $d) {
			$results[] = (string) $d;
		}
		
		if (isset($rest->body->ResponseMetadata->RequestId)) {
			$this->RequestId = (string) ($rest->body->ResponseMetadata->RequestId);
		}
		if (isset($rest->body->ResponseMetadata->BoxUsage)) {
			$this->BoxUsage = (string) ($rest->body->ResponseMetadata->BoxUsage);
		}
		
		return $results;
	}
	
	/**
	 * Get a domain's metadata
	 *
	 * @param string $domain The domain
	 * @return array | false
	 *	Array returned
	 *	(
	 *		[ItemCount] => 3
	 *		[ItemNamesSizeBytes] => 16
	 *		[AttributeNameCount] => 9
	 *		[AttributeNamesSizeBytes] => 76
	 *		[AttributeValueCount] => 13
	 *		[AttributeValuesSizeBytes] => 65
	 *		[Timestamp] => 1247238402
	 *	)
	 */
	public function domainMetadata($domain) {
		SimpleDB::__clearReturns();
		
		$rest = new SimpleDBRequest($domain, 'DomainMetadata', 'GET', self::$__accessKey);
		$rest = $rest->getResponse();
		if ($rest->error === false && $rest->code !== 200)
			$rest->error = array('code' => $rest->code, 'message' => 'Unexpected HTTP status');
		if ($rest->error !== false) {
			SimpleDB::__triggerError('domainMetadata', $rest->error);
			return false;
		}
		
		$results = array();
		if (! isset($rest->body->DomainMetadataResult)) {
			return $results;
		}
		if (isset($rest->body->DomainMetadataResult->ItemCount)) {
			$results['ItemCount'] = (string) ($rest->body->DomainMetadataResult->ItemCount);
		}
		if (isset($rest->body->DomainMetadataResult->ItemNamesSizeBytes)) {
			$results['ItemNamesSizeBytes'] = (string) ($rest->body->DomainMetadataResult->ItemNamesSizeBytes);
		}
		if (isset($rest->body->DomainMetadataResult->AttributeNameCount)) {
			$results['AttributeNameCount'] = (string) ($rest->body->DomainMetadataResult->AttributeNameCount);
		}
		if (isset($rest->body->DomainMetadataResult->AttributeNamesSizeBytes)) {
			$results['AttributeNamesSizeBytes'] = (string) ($rest->body->DomainMetadataResult->AttributeNamesSizeBytes);
		}
		if (isset($rest->body->DomainMetadataResult->AttributeValueCount)) {
			$results['AttributeValueCount'] = (string) ($rest->body->DomainMetadataResult->AttributeValueCount);
		}
		if (isset($rest->body->DomainMetadataResult->AttributeValuesSizeBytes)) {
			$results['AttributeValuesSizeBytes'] = (string) ($rest->body->DomainMetadataResult->AttributeValuesSizeBytes);
		}
		if (isset($rest->body->DomainMetadataResult->Timestamp)) {
			$results['Timestamp'] = (string) ($rest->body->DomainMetadataResult->Timestamp);
		}
		if (isset($rest->body->ResponseMetadata->RequestId)) {
			$this->RequestId = (string) ($rest->body->ResponseMetadata->RequestId);
		}
		if (isset($rest->body->ResponseMetadata->BoxUsage)) {
			$this->BoxUsage = (string) ($rest->body->ResponseMetadata->BoxUsage);
		}
		
		return $results;
	}
	
	/**
	 * Evaluate a select expression on a domain
	 *
	 * Function provided by Matthew Lanham
	 *
	 * @param string  $domain The domain being queried
	 * @param string  $select The select expression to evaluate.
	 * @param string  $nexttoken The token to start from when retrieving results
	 * @param boolean ConsistentRead - force consistent read = true
	 * @return array | false
	 */
	public function select($domain, $select, $nexttoken = null, $ConsistentRead = false) {
		SimpleDB::__clearReturns();
		
		$rest = new SimpleDBRequest($domain, 'Select', 'GET', self::$__accessKey);
		
		if ($select != '') {
			$rest->setParameter('SelectExpression', $select);
		}
		if ($nexttoken !== null) {
			$rest->setParameter('NextToken', $nexttoken);
		}
		if ($ConsistentRead == true) {
			$rest->setParameter('ConsistentRead', "true");
		}
		
		$rest = $rest->getResponse();
		
		if ($rest->error === false && $rest->code !== 200)
			$rest->error = array('code' => $rest->code, 'message' => 'Unexpected HTTP status');
		if ($rest->error !== false) {
			SimpleDB::__triggerError('query', $rest->error);
			return false;
		}
		
		$results = array();
		
		if (! isset($rest->body->SelectResult)) {
			return $results;
		}
		
		if (isset($rest->body->ResponseMetadata->RequestId)) {
			$this->RequestId = (string) ($rest->body->ResponseMetadata->RequestId);
		}
		if (isset($rest->body->ResponseMetadata->BoxUsage)) {
			$this->BoxUsage = (string) ($rest->body->ResponseMetadata->BoxUsage);
		}
		if ($rest->body->SelectResult->NextToken) {
			$this->NextToken = (string) $rest->body->SelectResult->NextToken;
		}
		
		foreach ($rest->body->SelectResult->Item as $i) {
			$item = array('Name' => (string) ($i->Name), 'Attributes' => array());
			foreach ($i->Attribute as $a) {
				if (isset($item['Attributes'][(string) ($a->Name)])) {
					$temp = (array) ($item['Attributes'][(string) ($a->Name)]);
					$temp[] = (string) ($a->Value);
					$item['Attributes'][(string) ($a->Name)] = $temp;
				} else {
					$item['Attributes'][(string) ($a->Name)] = (string) ($a->Value);
				}
			}
			$results[] = $item;
		}
		
		return $results;
	}
	
	/**
	 * Get attributes associated with an item
	 *
	 * @param string  $domain The domain containing the desired item
	 * @param string  $item The desired item
	 * @param integer $attribute A specific attribute to retrieve, or all if unspecified.
	 * @param boolean ConsistentRead - force consistent read = true
	 * @return boolean
	 */
	public function getAttributes($domain, $item, $attribute = null, $ConsistentRead = false) {
		SimpleDB::__clearReturns();
		
		$rest = new SimpleDBRequest($domain, 'GetAttributes', 'GET', self::$__accessKey);
		
		$rest->setParameter('ItemName', $item);
		
		if ($attribute !== null) {
			$rest->setParameter('AttributeName', $attribute);
		}
		
		if ($ConsistentRead == true) {
			$rest->setParameter('ConsistentRead', "true");
		}
		
		$rest = $rest->getResponse();
		if ($rest->error === false && $rest->code !== 200)
			$rest->error = array('code' => $rest->code, 'message' => 'Unexpected HTTP status');
		if ($rest->error !== false) {
			SimpleDB::__triggerError('getAttributes', $rest->error);
			return false;
		}
		
		$results = array();
		if (! isset($rest->body->GetAttributesResult)) {
			return $results;
		}
		
		foreach ($rest->body->GetAttributesResult->Attribute as $a) {
			if (isset($results[(string) ($a->Name)])) {
				$temp = (array) ($results[(string) ($a->Name)]);
				$temp[] = (string) ($a->Value);
				$results[(string) ($a->Name)] = $temp;
			} else {
				$results[(string) ($a->Name)] = (string) ($a->Value);
			}
		}
		
		if (isset($rest->body->ResponseMetadata->RequestId)) {
			$this->RequestId = (string) ($rest->body->ResponseMetadata->RequestId);
		}
		if (isset($rest->body->ResponseMetadata->BoxUsage)) {
			$this->BoxUsage = (string) ($rest->body->ResponseMetadata->BoxUsage);
		}
		
		return $results;
	}
	
	/**
	 * Create or update attributes on an item
	 *
	 * @param string  $domain The domain containing the desired item
	 * @param string  $item The desired item
	 * @param array $attributes An array of (name => (value [, replace])),
	 *							 where replace is a boolean of whether to replace the item.
	 *							 replace is optional, and defaults to false.
	 *							 If value is an array, multiple values are put.
	 * @param array $expected An array of (name => (value)), or (name => (exists = "false"))
	 * @return boolean
	 */
	public function putAttributes($domain, $item, $attributes, $expected = null) {
		SimpleDB::__clearReturns();
		
		$rest = new SimpleDBRequest($domain, 'PutAttributes', 'POST', self::$__accessKey);
		
		$rest->setParameter('ItemName', $item);
		
		$i = 0;
		foreach ($attributes as $name => $v) {
			if (is_array($v['value'])) {
				foreach ($v['value'] as $val) {
					$rest->setParameter('Attribute.' . $i . '.Name', $name);
					$rest->setParameter('Attribute.' . $i . '.Value', $val, false);
					
					if (isset($v['replace'])) {
						$rest->setParameter('Attribute.' . $i . '.Replace', $v['replace']);
					}
					$i++;
				}
			} else {
				$rest->setParameter('Attribute.' . $i . '.Name', $name);
				$rest->setParameter('Attribute.' . $i . '.Value', $v['value']);
				if (isset($v['replace'])) {
					$rest->setParameter('Attribute.' . $i . '.Replace', $v['replace']);
				}
				$i++;
			}
		}
		
		if (is_array($expected)) {
			foreach ($expected as $name => $v) {
				// PAP
				if (key_exists('value', $v)) {
					
					if (is_array($v['value'])) { // expected value
						foreach ($v['value'] as $val) {
							$rest->setParameter('Expected.' . $i . '.Name', $name);
							$rest->setParameter('Expected.' . $i . '.Value', $val);
							$i++;
						}
					} else {
						if ($v['value']) {
							$rest->setParameter('Expected.' . $i . '.Name', $name);
							$rest->setParameter('Expected.' . $i . '.Value', $v['value']);
							$i++;
						}
					}
				
				// PAP
				}
				if (key_exists('exists', $v)) {
				
					if (is_array($v['exists'])) {
						foreach ($v['exists'] as $val) { // expected does not exist
							$rest->setParameter('Expected.' . $i . '.Name', $name);
							$rest->setParameter('Expected.' . $i . '.Exists', $val);
							$i++;
						}
					} else {
						if ($v['exists']) {
							$rest->setParameter('Expected.' . $i . '.Name', $name);
							$rest->setParameter('Expected.' . $i . '.Exists', $v['exists']);
							$i++;
						}
					}
				
				}
			}
		}
		
		$rest = $rest->getResponse();
		if ($rest->error === false && $rest->code !== 200)
			$rest->error = array('code' => $rest->code, 'message' => 'Unexpected HTTP status');
		if ($rest->error !== false) {
			SimpleDB::__triggerError('putAttributes', $rest->error);
			return false;
		}
		if (isset($rest->body->ResponseMetadata->RequestId)) {
			$this->RequestId = (string) ($rest->body->ResponseMetadata->RequestId);
		}
		if (isset($rest->body->ResponseMetadata->BoxUsage)) {
			$this->BoxUsage = (string) ($rest->body->ResponseMetadata->BoxUsage);
		}
		
		return true;
	}
	
	/**
	 * Create or update attributes on multiple item
	 * MAX 35 items per write (SimpleDB limit)
	 *
	 * Function provided by Matthew Lanham
	 *
	 * @param string $domain The domain containing the desired item
	 * @param array  $items An array of items of (item_name, attributes => array(attribute_name => array(value [, replace])))
	 *	If replace is omitted it defaults to false.
	 *	Optionally, attributes may just be a single string value, and replace will default to false.
	 * @return boolean
	 */
	public function batchPutAttributes($domain, $items) {
		SimpleDB::__clearReturns();
		
		$rest = new SimpleDBRequest($domain, 'BatchPutAttributes', 'POST', self::$__accessKey);
		
		$ii = 0;
		foreach ($items as $item) {
			$rest->setParameter('Item.' . $ii . '.ItemName', $item['name']);
			
			$i = 0;
			foreach ($item['attributes'] as $name => $v) {
				if (is_array($v['value'])) {
					foreach ($v['value'] as $val) {
						$rest->setParameter('Item.' . $ii . '.Attribute.' . $i . '.Name', $name);
						$rest->setParameter('Item.' . $ii . '.Attribute.' . $i . '.Value', $val, false);
						
						if (isset($v['replace'])) {
							$rest->setParameter('Item.' . $ii . '.Attribute.' . $i . '.Replace', $v['replace']);
						}
						$i++;
					}
				} else {
					$rest->setParameter('Item.' . $ii . '.Attribute.' . $i . '.Name', $name);
					$rest->setParameter('Item.' . $ii . '.Attribute.' . $i . '.Value', $v['value']);
					if (isset($v['replace'])) {
						$rest->setParameter('Item.' . $ii . '.Attribute.' . $i . '.Replace', $v['replace']);
					}
					$i++;
				}
			}
			$ii++;
		}
		
		$rest = $rest->getResponse();
		if ($rest->error === false && $rest->code !== 200)
			$rest->error = array('code' => $rest->code, 'message' => 'Unexpected HTTP status');
		if ($rest->error !== false) {
			SimpleDB::__triggerError('batchPutAttributes', $rest->error);
			return false;
		}
		
		if (isset($rest->body->ResponseMetadata->RequestId)) {
			$this->RequestId = (string) ($rest->body->ResponseMetadata->RequestId);
		}
		if (isset($rest->body->ResponseMetadata->BoxUsage)) {
			$this->BoxUsage = (string) ($rest->body->ResponseMetadata->BoxUsage);
		}
		
		return true;
	}
	
	/**
	 * Delete attributes associated with an item
	 *
	 * @param string  $domain The domain containing the desired item
	 * @param string  $item The desired item
	 * @param integer $attributes An array of (name => value)
	 *				value is either a specific value or null.
	 *				setting the value will erase the attribute with the given
	 *				name and value (for multi-valued attributes).
	 *				If array is unspecified, all attributes are deleted.
	 * @return boolean
	 */
	public function deleteAttributes($domain, $item, $attributes = null, $expected = null) {
		SimpleDB::__clearReturns();
		
		$rest = new SimpleDBRequest($domain, 'DeleteAttributes', 'DELETE', self::$__accessKey);
		
		$rest->setParameter('ItemName', $item);
		
		$i = 0;
		if ($attributes !== null) {
			foreach ($attributes as $name => $value) {
				$rest->setParameter('Attribute.' . $i . '.Name', $name);
				if ($value !== null) {
					$rest->setParameter('Attribute.' . $i . '.Value', $value);
				}
				$i++;
			}
		}
		
		if (is_array($expected)) {
			foreach ($expected as $name => $v) {
				if (is_array($v['value'])) { // expected value
					foreach ($v['value'] as $val) {
						$rest->setParameter('Expected.' . $i . '.Name', $name);
						$rest->setParameter('Expected.' . $i . '.Value', $val);
						$i++;
					}
				} else {
					if ($v['value']) {
						$rest->setParameter('Expected.' . $i . '.Name', $name);
						$rest->setParameter('Expected.' . $i . '.Value', $v['value']);
						$i++;
					}
				}
				if (is_array($v['exists'])) {
					foreach ($v['exists'] as $val) { // expected does not exist
						$rest->setParameter('Expected.' . $i . '.Name', $name);
						$rest->setParameter('Expected.' . $i . '.Exists', $val);
						$i++;
					}
				} else {
					if ($v['exists']) {
						$rest->setParameter('Expected.' . $i . '.Name', $name);
						$rest->setParameter('Expected.' . $i . '.Exists', $v['exists']);
						$i++;
					}
				}
			}
		}
		
		$rest = $rest->getResponse();
		if ($rest->error === false && $rest->code !== 200)
			$rest->error = array('code' => $rest->code, 'message' => 'Unexpected HTTP status');
		if ($rest->error !== false) {
			SimpleDB::__triggerError('deleteAttributes', $rest->error);
			return false;
		}
		
		if (isset($rest->body->ResponseMetadata->RequestId)) {
			$this->RequestId = (string) ($rest->body->ResponseMetadata->RequestId);
		}
		if (isset($rest->body->ResponseMetadata->BoxUsage)) {
			$this->BoxUsage = (string) ($rest->body->ResponseMetadata->BoxUsage);
		}
		
		return true;
	}
	
	/**
	 * Clear public parameters
	 *
	 */
	public function __clearReturns() {
		$this->BoxUsage = null;
		$this->RequestId = null;
		$this->NextToken = null;
		$this->ErrorCode = null;
		return true;
	}
	
	/**
	 * Trigger an error message
	 *
	 * @internal Used by member functions to output errors
	 * @param array $error Array containing error information
	 * @return string
	 */
	public function __triggerError($functionname, $error) {
		if ($error['curl']) {
			$this->ErrorCode = $error['code'];
			trigger_error(sprintf("SimpleDB::%s(): %s %s", $functionname, $error['code'], $error['message']), E_USER_WARNING);	
		} else {
			foreach ($error['Errors'] as $e) {
				$message = sprintf("SimpleDB::%s(): %s: %s\n", $functionname, $e['Code'], $e['Message']);
				$this->ErrorCode = $e['Code'];
				// PAP		
				if (!key_exists($e['Code'], SimpleDB::$handledErrors)) {
					trigger_error($message, E_USER_WARNING);
				}
			}
		}
	}
	
	/**
	 * Generate the auth string using Hmac-SHA256
	 *
	 * @internal Used by SimpleDBRequest::getResponse()
	 * @param string $string String to sign
	 * @return string
	 */
	public static function __getSignature($string) {
		return base64_encode(hash_hmac('sha256', $string, self::$__secretKey, true));
	}
}

final class SimpleDBRequest {
	private $sdbhost, $verb, $resource = '', $parameters = array();
	public $response;
	
	/**
	 * Constructor
	 *
	 * @param string $domain Domain name
	 * @param string $action SimpleDB action
	 * @param string $verb HTTP verb
	 * @param string $accesskey AWS Access Key
	 * @param string $host The URL of the SimpleDB host
	 * @return mixed
	 */
	function __construct($domain, $action, $verb, $accesskey, $host = 'sdb.amazonaws.com') {
		if ($domain != '') {
			$this->parameters['DomainName'] = $domain;
		}
		
		$this->parameters['Action'] = $action;
		$this->parameters['Version'] = '2009-04-15';
		$this->parameters['SignatureVersion'] = '2';
		$this->parameters['SignatureMethod'] = 'HmacSHA256';
		$this->parameters['AWSAccessKeyId'] = $accesskey;
		
		$this->verb = $verb;
		$this->sdbhost = $host;
		$this->response = new STDClass();
		$this->response->error = false;
	}
	
	/**
	 * Set request parameter
	 *
	 * @param string  $key Key
	 * @param string  $value Value
	 * @param boolean $replace Whether to replace the key if it already exists (default true)
	 * @return void
	 */
	public function setParameter($key, $value, $replace = true) {
		if (! $replace && isset($this->parameters[$key])) {
			$temp = (array) ($this->parameters[$key]);
			$temp[] = $value;
			$this->parameters[$key] = $temp;
		} else {
			$this->parameters[$key] = $value;
		}
	}
	
	/**
	 * Get the response
	 *
	 * @return object | false
	 */
	public function getResponse() {
		
		$this->parameters['Timestamp'] = gmdate('c');
		
		$params = array();
		foreach ($this->parameters as $var => $value) {
			if (is_array($value)) {
				foreach ($value as $v) {
					$params[] = $var . '=' . self::__customUrlEncode($v);
				}
			} else {
				$params[] = $var . '=' . self::__customUrlEncode($value);
			}
		}
		
		sort($params, SORT_STRING);
		
		$query = implode('&', $params);
		
		$strtosign = $this->verb . "\n" . $this->sdbhost . "\n/\n" . $query;
		$query .= '&Signature=' . self::__customUrlEncode(SimpleDB::__getSignature($strtosign));
		
		$ssl = (SimpleDB::$useSSL && extension_loaded('openssl'));
		$url = ($ssl ? 'https://' : 'http://') . $this->sdbhost . '/?' . $query;
		
		// Basic setup
		$curl = curl_init();
		curl_setopt($curl, CURLOPT_USERAGENT, 'SimpleDB/php');
		
		if (SimpleDB::$useSSL) {
			curl_setopt($curl, CURLOPT_SSL_VERIFYHOST, SimpleDB::$verifyHost);
			curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, SimpleDB::$verifyPeer);
		}
		
		curl_setopt($curl, CURLOPT_URL, $url);
		curl_setopt($curl, CURLOPT_HEADER, false);
		curl_setopt($curl, CURLOPT_RETURNTRANSFER, false);
		curl_setopt($curl, CURLOPT_WRITEFUNCTION, array(&$this, '__responseWriteCallback'));
		curl_setopt($curl, CURLOPT_FOLLOWLOCATION, true);
		
		// Request types
		switch ($this->verb) {
			case 'GET':
				break;
			case 'PUT':
			case 'POST':
				curl_setopt($curl, CURLOPT_CUSTOMREQUEST, $this->verb);
				break;
			case 'HEAD':
				curl_setopt($curl, CURLOPT_CUSTOMREQUEST, 'HEAD');
				curl_setopt($curl, CURLOPT_NOBODY, true);
				break;
			case 'DELETE':
				curl_setopt($curl, CURLOPT_CUSTOMREQUEST, 'DELETE');
				break;
			default:
				break;
		}
		
		// Execute, grab errors
		if (curl_exec($curl)) {
			$this->response->code = curl_getinfo($curl, CURLINFO_HTTP_CODE);
		} else {
			$this->response->error = array('curl' => true, 'code' => curl_errno($curl), 'message' => curl_error($curl), 'resource' => $this->resource);
			$this->response->rawXML = "";
		}
		
		@curl_close($curl);
		
		// Parse body into XML
		if ($this->response->error === false && isset($this->response->body)) {
			$this->response->rawXML = (string) $this->response->body;
			$this->response->body = simplexml_load_string($this->response->body);
			
			// Grab SimpleDB errors
			if (! in_array($this->response->code, array(200, 204)) && isset($this->response->body->Errors)) {
				$this->response->error = array('curl' => false, 'Errors' => array());
				foreach ($this->response->body->Errors->Error as $e) {
					$err = array('Code' => (string) ($e->Code), 'Message' => (string) ($e->Message), 'BoxUsage' => (string) ($e->BoxUsage));
					$this->response->error['Errors'][] = $err;
				}
				unset($this->response->body);
			}
		}
		
		return $this->response;
	}
	
	/**
	 * CURL write callback
	 *
	 * @param resource &$curl CURL resource
	 * @param string &$data Data
	 * @return integer
	 */
	private function __responseWriteCallback(&$curl, &$data) {
		$this->response->body .= $data;
		return strlen($data);
	}
	
	/**
	 * Contributed by afx114
	 * URL encode the parameters as per http://docs.amazonwebservices.com/AWSECommerceService/latest/DG/index.html?Query_QueryAuth.html
	 * PHP's rawurlencode() follows RFC 1738, not RFC 3986 as required by Amazon. The only difference is the tilde (~), so convert it back after rawurlencode
	 * See: http://www.morganney.com/blog/API/AWS-Product-Advertising-API-Requires-a-Signed-Request.php
	 *
	 * @param string $var String to encode
	 * @return string
	 */
	private function __customUrlEncode($var) {
		return str_replace('%7E', '~', rawurlencode($var));
	}
}

?>