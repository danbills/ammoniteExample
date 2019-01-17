helm install --set-file google.service_account_json=sa.json --set google.service_account_id=$( jq .client_email < sa.json ) ./
