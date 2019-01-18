#!/usr/bin/env bash

# This script is designed to be run by Jenkins to deploy a new Job Manager kubernetes deployment
# =======================================
# Example Usage:
# bash deploy.sh broad-dsde-mint-dev gke_broad-dsde-mint-dev_us-central1-b_lira v0.0.4 \
# "https://cromwell.mint-dev.broadinstitute.org/api/workflows/v1" false true username password dev
# =======================================



# The main function to execute all steps of a deployment of Job Manager
function main() {
    local GCLOUD_PROJECT=$1
    local GKE_CONTEXT=$2
    local JMUI_TAG=$3
    local CROMWELL_URL=$4
    local USE_CAAS=$5
    local USE_PROXY=$6
    local JMUI_USR=$7
    local JMUI_PWD=$8
    local VAULT_ENV=$9
    local CLIENT_ID=${10:-""}
    local VAULT_TOKEN_FILE=${11:-"$HOME/.vault-token"}

    local DOCKER_TAG=${JMUI_TAG}
    local API_DOCKER_IMAGE="databiosphere/job-manager-api-cromwell:${DOCKER_TAG}"
    local UI_DOCKER_IMAGE="databiosphere/job-manager-ui:${DOCKER_TAG}"

    set -e

    line
    configure_kubernetes ${GCLOUD_PROJECT} ${GKE_CONTEXT}

    local API_CONFIG="cromwell-credentials-$(date '+%Y-%m-%d-%H-%M')"
    local JM_CONFIGMAP_OBJ="jm-configmap-$(date '+%Y-%m-%d-%H-%M')"

    local UI_PROXY="jm-htpasswd-$(date '+%Y-%m-%d-%H-%M')"

    if [ ${USE_PROXY} == "true" ]; then
        local USERNAME=${JMUI_USR}
        local PASSWORD=${JMUI_PWD}
        if create_UI_proxy ${USERNAME} ${PASSWORD} ${UI_PROXY}
        then
            stdout "Successfully created UI proxy."
        else
            tear_down_kube_secret ${UI_PROXY}
            stderr
        fi
    fi

    if [ ${USE_CAAS} == "false" ]; then
        if create_API_config ${VAULT_ENV} ${API_CONFIG} ${VAULT_TOKEN_FILE}
        then
            stdout "Successfully created API config."
        else
            tear_down_kube_secret ${API_CONFIG}
            stderr
        fi
    fi

    line
    render_UI_config ${CLIENT_ID}

    line
    render_NGINX_conf ${JMUI_TAG} ${USE_PROXY}

    if create_jm_configmap_obj ${JM_CONFIGMAP_OBJ} ${USE_CAAS}
    then
        stdout "Successfully created UI configMap object."
    else
        tear_down_kube_configMap ${JM_CONFIGMAP_OBJ}
        stderr
    fi

    line
    apply_kube_service

    line
    apply_kube_deployment ${CROMWELL_URL} ${API_DOCKER_IMAGE} ${API_CONFIG} ${JM_CONFIGMAP_OBJ} ${UI_DOCKER_IMAGE} ${UI_PROXY} ${USE_CAAS} ${USE_PROXY}

#    line
#    Each re-deployment to the ingress will cause a ~10 minuted downtime to the Job Manager. So this script assumes that you have created your ingress before using this it. This functions is here just for completeness.
#    TODO: Add back the ingress set up step if needed
#    apply_kube_ingress ${TLS_SECRET_NAME}

    line
    tear_down_rendered_files
}

# Main Runner:
error=0
if [ -z $1 ]; then
    echo -e "\nYou must specify a gcloud project to use for the deployment!"
    error=1
fi

if [ -z $2 ]; then
    echo -e "\nYou must specify the gke context to use for the deployment! E.g. gke_{gcloud-project-id}_{zone}_{clustername}"
    error=1
fi

if [ -z $3 ]; then
    echo -e "\nYou must specify a Job Manager Git Tag!"
    error=1
fi

if [ -z $4 ]; then
    echo -e "\nYou must specify the url for the Cromwell instance to use with the Job Manager UI!"
    error=1
fi

if [ -z $5 ]; then
    echo -e "\nYou must specify whether to use Cromwell-as-a-Service with Job Manager UI!"
    error=1
fi

if [ -z $6 ]; then
    echo -e "\nYou must specify whether to use a UI proxy!"
    error=1
fi

if [ -z $7 ]; then
    echo -e "\nYou must specify a desired username for Job Manager UI in order to use a UI proxy!"
    error=1
fi

if [ -z $8 ]; then
    echo -e "\nYou must specify a desired password for Job Manager UI in order to use a UI proxy!"
    error=1
fi

if [ -z $9 ]; then
    echo -e "\nYou must specify the deployment environment for retrieving Cromwell credentials from vault, if not using Cromwell-as-a-Service!"
    error=1
fi

if [ -z ${10} ]; then
    echo -e "\nYou must specify a Client ID if authentication is required in the capabilities config, using default value ''."
fi

if [ -z ${11} ]; then
    echo -e "\nMissing the Vault token file parameter, using default value $HOME/.vault-token. Otherwise, pass in the path to the token file as the 9th argument of this script!"
fi


if [ $error -eq 1 ]; then
    echo -e "\nUsage: bash deploy.sh GCLOUD_PROJECT GKE_CONTEXT JMUI_TAG CROMWELL_URL USE_CAAS USE_PROXY JMUI_USR JMUI_PWD VAULT_ENV(dev/staging/test) CLIENT_ID(optional) VAULT_TOKEN_FILE(optional)\n"
    exit 1
fi

main $1 $2 $3 $4 $5 $6 $7 $8 $9 ${10} ${11}
