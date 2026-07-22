from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
from sklearn.ensemble import IsolationForest
import numpy as np
import logging

app = Flask(__name__)
CORS(app)

logging.basicConfig(level=logging.INFO)

model = None

def get_model():
    global model
    if model is None:
        model = IsolationForest(
            contamination=0.05,
            random_state=42,
            n_estimators=100
        )
    return model

def prepare_features(row):
    features = []
    amount = float(row.get('montant', 0))
    features.append(amount)
    msisdn = str(row.get('msisdn', ''))
    features.append(len(msisdn))
    features.append(sum(c.isdigit() for c in msisdn))
    features.append(len(str(row.get('prenom', ''))))
    features.append(len(str(row.get('nom', ''))))
    features.append(len(str(row.get('remark', ''))))
    return features

@app.route('/analyze', methods=['POST'])
def analyze_csv():
    try:
        if 'file' not in request.files:
            return jsonify({'error': 'Aucun fichier'}), 400
        file = request.files['file']
        if file.filename == '':
            return jsonify({'error': 'Nom vide'}), 400

        df = pd.read_csv(file)
        app.logger.info(f"📊 Analyse de {len(df)} lignes")

        required = ['msisdn', 'matricule', 'montant', 'prenom', 'nom']
        for col in required:
            if col not in df.columns:
                return jsonify({'error': f'Colonne manquante : {col}'}), 400

        errors = []
        for idx, row in df.iterrows():
            line = idx + 2
            row_errors = []
            msisdn = str(row.get('msisdn', '')).strip()
            if len(msisdn) != 8 or not msisdn.isdigit():
                row_errors.append('Numéro invalide (8 chiffres)')
            try:
                amount = float(row.get('montant', 0))
                if amount <= 0:
                    row_errors.append('Montant doit être > 0')
                elif amount > 1000000:
                    row_errors.append('Montant > 1M TND')
            except:
                row_errors.append('Montant invalide')
            if not row.get('prenom') or str(row.get('prenom')).strip() == '':
                row_errors.append('Prénom manquant')
            if not row.get('nom') or str(row.get('nom')).strip() == '':
                row_errors.append('Nom manquant')
            if not row.get('matricule') or str(row.get('matricule')).strip() == '':
                row_errors.append('Matricule manquant')
            if row_errors:
                errors.append({'line': line, 'errors': row_errors, 'data': row.to_dict()})

        features = []
        valid_idx = []
        for idx, row in df.iterrows():
            try:
                features.append(prepare_features(row))
                valid_idx.append(idx)
            except:
                pass

        if len(features) > 10:
            model = get_model()
            arr = np.array(features)
            preds = model.fit_predict(arr)
            for i, p in enumerate(preds):
                if p == -1:
                    line = valid_idx[i] + 2
                    existing = next((e for e in errors if e['line'] == line), None)
                    if existing:
                        existing['errors'].append('⚠️ Anomalie IA')
                    else:
                        errors.append({
                            'line': line,
                            'errors': ['⚠️ Anomalie IA'],
                            'data': df.iloc[valid_idx[i]].to_dict()
                        })

        app.logger.info(f"✅ {len(errors)} erreurs trouvées")
        return jsonify({
            'total_lines': len(df),
            'errors': errors,
            'has_errors': len(errors) > 0,
            'error_count': len(errors)
        })

    except Exception as e:
        app.logger.error(f"❌ Erreur : {e}")
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)