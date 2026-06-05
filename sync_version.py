#!/usr/bin/env python3
import sys
import re
import json
import os
from datetime import date

# Colors for nice output
GREEN = '\033[0;32m'
BLUE = '\033[0;34m'
YELLOW = '\033[1;33m'
RED = '\033[0;31m'
NC = '\033[0m'

def print_step(msg):
    print(f"{BLUE}➔ {msg}{NC}")

def print_success(msg):
    print(f"{GREEN}✓ {msg}{NC}")

def print_warning(msg):
    print(f"{YELLOW}⚠ {msg}{NC}")

def print_error(msg):
    print(f"{RED}✗ {msg}{NC}")

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)

    # 1. Determine target version
    gradle_props_path = "gradle.properties"
    
    if not os.path.exists(gradle_props_path):
        print_error(f"Could not find {gradle_props_path} in current directory.")
        sys.exit(1)

    # Read current version from gradle.properties
    with open(gradle_props_path, "r") as f:
        content = f.read()
    
    match = re.search(r"^version\s*=\s*([0-9a-zA-Z.-]+)", content, re.MULTILINE)
    if not match:
        print_error("Could not find 'version = ...' line in gradle.properties.")
        sys.exit(1)
    
    current_version = match.group(1)

    if len(sys.argv) > 1:
        target_version = sys.argv[1].strip()
        if not re.match(r"^[0-9a-zA-Z.-]+$", target_version):
            print_error(f"Invalid version format: '{target_version}'")
            sys.exit(1)
    else:
        target_version = current_version
        print_step(f"No version argument provided. Syncing files using current version: {target_version}")

    # 2. Update gradle.properties if changed
    if target_version != current_version:
        print_step(f"Updating gradle.properties: {current_version} ➔ {target_version}")
        updated_content = re.sub(
            r"^version\s*=\s*[0-9a-zA-Z.-]+", 
            f"version = {target_version}", 
            content, 
            flags=re.MULTILINE
        )
        with open(gradle_props_path, "w") as f:
            f.write(updated_content)
        print_success("gradle.properties updated successfully.")
    else:
        print_success(f"gradle.properties is already at version {target_version}")

    # 3. Update patches-list.json
    patches_list_path = "patches-list.json"
    if os.path.exists(patches_list_path):
        print_step(f"Syncing {patches_list_path}...")
        with open(patches_list_path, "r") as f:
            try:
                data = json.load(f)
            except Exception as e:
                print_error(f"Failed to parse {patches_list_path}: {e}")
                sys.exit(1)
        
        if data.get("version") != target_version:
            data["version"] = target_version
            with open(patches_list_path, "w") as f:
                json.dump(data, f, indent=2)
                f.write("\n") # standard ending newline
            print_success(f"{patches_list_path} updated.")
        else:
            print_success(f"{patches_list_path} is already in sync.")
    else:
        print_warning(f"{patches_list_path} not found.")

    # 4. Update patches-bundle.json
    patches_bundle_path = "patches-bundle.json"
    if os.path.exists(patches_bundle_path):
        print_step(f"Syncing {patches_bundle_path}...")
        with open(patches_bundle_path, "r") as f:
            try:
                data = json.load(f)
            except Exception as e:
                print_error(f"Failed to parse {patches_bundle_path}: {e}")
                sys.exit(1)

        today_str = date.today().isoformat()
        
        updated = False

        if data.get("version") != target_version:
            data["version"] = target_version
            updated = True
        
        expected_download_url = f"https://github.com/quantavil/edge-morphe-patches/releases/download/v{target_version}/patches-{target_version}.mpp"
        if data.get("download_url") != expected_download_url:
            data["download_url"] = expected_download_url
            updated = True

        # Update description release markdown header and date
        desc = data.get("description", "")
        # Pattern matches: ## [X.Y.Z](url) (YYYY-MM-DD)
        desc_pattern = r"## \[[0-9a-zA-Z.-]+\]\(https://github.com/quantavil/edge-morphe-patches/releases/tag/v[0-9a-zA-Z.-]+\) \(\d{4}-\d{2}-\d{2}\)"
        new_desc_header = f"## [{target_version}](https://github.com/quantavil/edge-morphe-patches/releases/tag/v{target_version}) ({today_str})"
        
        if re.search(desc_pattern, desc):
            new_desc = re.sub(desc_pattern, new_desc_header, desc)
            if new_desc != desc:
                data["description"] = new_desc
                updated = True
        else:
            # If pattern not found, prepending or logging warning
            print_warning(f"Could not find release header pattern in description. Adding default header.")
            data["description"] = f"{new_desc_header}\n\n{desc}"
            updated = True

        if updated:
            with open(patches_bundle_path, "w") as f:
                json.dump(data, f, indent=2)
                f.write("\n")
            print_success(f"{patches_bundle_path} updated.")
        else:
            print_success(f"{patches_bundle_path} is already in sync.")
    else:
        print_warning(f"{patches_bundle_path} not found.")

    # 5. Update README.md
    readme_path = "README.md"
    if os.path.exists(readme_path):
        print_step(f"Syncing {readme_path}...")
        with open(readme_path, "r") as f:
            content = f.read()

        readme_pattern = r"\*\*\[v[0-9a-zA-Z.-]+\]\(https://github.com/quantavil/edge-morphe-patches/releases/tag/v[0-9a-zA-Z.-]+\)\*\*"
        new_readme_tag = f"**[v{target_version}](https://github.com/quantavil/edge-morphe-patches/releases/tag/v{target_version})**"

        if re.search(readme_pattern, content):
            updated_content = re.sub(readme_pattern, new_readme_tag, content)
            if updated_content != content:
                with open(readme_path, "w") as f:
                    f.write(updated_content)
                print_success(f"{readme_path} updated.")
            else:
                print_success(f"{readme_path} is already in sync.")
        else:
            print_warning(f"Could not find version tag pattern in {readme_path}.")
    else:
        print_warning(f"{readme_path} not found.")

if __name__ == "__main__":
    main()
