#!/bin/bash

echo "================================================"
echo "BananaCreat - Pushing to GitHub"
echo "================================================"
echo ""
echo "Repository: https://github.com/gostgamze-commits/bananacreat"
echo ""

# Configure git
git config user.email "dev@bananacreat.local"
git config user.name "BananaCreat Developer"

# Add remote
git remote remove origin 2>/dev/null || true
git remote add origin https://github.com/gostgamze-commits/bananacreat.git

# Ensure we're on main branch
git branch -M main

# Push to GitHub
echo "Pushing to GitHub..."
git push -u origin main --force 2>&1 | tail -20

echo ""
echo "================================================"
echo "✅ Push Complete!"
echo "================================================"
echo ""
echo "Repository: https://github.com/gostgamze-commits/bananacreat"
echo ""
echo "Next steps:"
echo "1. Go to: https://github.com/gostgamze-commits/bananacreat/actions"
echo "2. Wait for GitHub Actions to build (10-15 minutes)"
echo "3. Download APK from artifacts"
echo ""
